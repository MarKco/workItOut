package com.ilsecondodasinistra.workitout.domain

import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Hours worked against the target over a period. Only days that have an entry are counted. */
data class PeriodStats(
    val workedMillis: Long = 0,
    val targetMillis: Long = 0,
    val daysWorked: Int = 0,
) {
    /** Positive = overtime, negative = hours owed. */
    val balanceMillis: Long get() = workedMillis - targetMillis
    val averageMillis: Long get() = if (daysWorked == 0) 0 else workedMillis / daysWorked
}

data class DayBar(
    val date: LocalDate,
    val workedMillis: Long,
    val targetMillis: Long,
    val isWorkDay: Boolean,
)

data class Stats(
    val week: PeriodStats,
    val month: PeriodStats,
    val overall: PeriodStats,
    /** Oldest first, ending today. */
    val recentDays: List<DayBar>,
)

object WorkStats {

    private const val MS_PER_HOUR = 3_600_000.0

    /**
     * @param workDays days that count towards the target; on any other day the target is 0, so
     * everything worked then is overtime.
     */
    fun compute(
        history: List<WorkHistoryEntry>,
        workDays: Set<DayOfWeek>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault(),
        recentDayCount: Int = 14,
    ): Stats {
        val days: Map<LocalDate, DayBar> = history
            .groupBy { java.time.Instant.ofEpochMilli(it.enterTime).atZone(zone).toLocalDate() }
            .mapValues { (date, entries) ->
                val isWorkDay = date.dayOfWeek in workDays
                val worked = entries.sumOf {
                    WorkTimeCalculator.workedMillis(it.enterTime, it.exitTime, it.pauses) ?: 0
                }
                val target = if (isWorkDay) (entries.maxOf { it.dailyHoursTarget } * MS_PER_HOUR).toLong() else 0
                DayBar(date, worked, target, isWorkDay)
            }

        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val month = YearMonth.from(today)

        fun summarize(filter: (LocalDate) -> Boolean) = days.values
            .filter { filter(it.date) }
            .fold(PeriodStats()) { acc, day ->
                PeriodStats(acc.workedMillis + day.workedMillis, acc.targetMillis + day.targetMillis, acc.daysWorked + 1)
            }

        val recent = (recentDayCount - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            days[date] ?: DayBar(date, 0, 0, date.dayOfWeek in workDays)
        }

        return Stats(
            week = summarize { it in weekStart..weekEnd },
            month = summarize { YearMonth.from(it) == month },
            overall = summarize { true },
            recentDays = recent,
        )
    }
}
