package com.ilsecondodasinistra.workitout.domain

import com.ilsecondodasinistra.workitout.data.PausePair
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class PauseDraft(val start: LocalTime? = null, val end: LocalTime? = null)

enum class DraftError { PauseIncomplete, PauseOutsideDay, ExitNotAfterEnter, InvalidTarget }

sealed interface DraftResult {
    data class Valid(val entry: WorkHistoryEntry) : DraftResult
    data class Invalid(val error: DraftError) : DraftResult
}

/**
 * A work day as the user edits it: one date plus clock times. Turned into real timestamps by
 * [toEntry], where a time earlier than the previous one means "the next day" (night shifts).
 */
data class EntryDraft(
    val date: LocalDate,
    val enter: LocalTime,
    val exit: LocalTime,
    val pauses: List<PauseDraft> = emptyList(),
    val targetHours: Double,
) {
    fun toEntry(id: String?, zone: ZoneId = ZoneId.systemDefault()): DraftResult {
        if (targetHours !in 0.0..24.0) return DraftResult.Invalid(DraftError.InvalidTarget)

        fun millis(time: LocalTime) = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
        fun rollAfter(time: LocalTime, reference: Long) =
            WorkTimeCalculator.rollToNextDayIfBefore(millis(time), reference, zone)

        val enterMillis = millis(enter)
        val exitMillis = rollAfter(exit, enterMillis)
        if (exitMillis == enterMillis) return DraftResult.Invalid(DraftError.ExitNotAfterEnter)

        val resolved = mutableListOf<PausePair>()
        for (draft in pauses) {
            if (draft.start == null && draft.end == null) continue
            val startTime = draft.start
            val endTime = draft.end
            if (startTime == null || endTime == null) return DraftResult.Invalid(DraftError.PauseIncomplete)
            val start = rollAfter(startTime, enterMillis)
            val end = rollAfter(endTime, start)
            if (end > exitMillis) return DraftResult.Invalid(DraftError.PauseOutsideDay)
            resolved += PausePair(start, end)
        }
        resolved.sortBy { it.start }

        val worked = WorkTimeCalculator.workedMillis(enterMillis, exitMillis, resolved) ?: 0
        return DraftResult.Valid(
            WorkHistoryEntry(
                id = id ?: enterMillis.toString(),
                enterTime = enterMillis,
                pauses = resolved,
                exitTime = exitMillis,
                totalWorkedTime = WorkTimeCalculator.formatDuration(worked),
                dailyHoursTarget = targetHours,
            ),
        )
    }

    companion object {
        fun fromEntry(entry: WorkHistoryEntry, zone: ZoneId = ZoneId.systemDefault()): EntryDraft {
            fun time(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime().withSecond(0).withNano(0)
            return EntryDraft(
                date = Instant.ofEpochMilli(entry.enterTime).atZone(zone).toLocalDate(),
                enter = time(entry.enterTime),
                exit = time(entry.exitTime),
                pauses = entry.pauses.map { PauseDraft(it.start?.let(::time), it.end?.let(::time)) },
                targetHours = entry.dailyHoursTarget,
            )
        }
    }
}
