package com.ilsecondodasinistra.workitout.domain

import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/** Machine readable export: ISO dates, 24h times, durations in whole minutes. Oldest day first. */
object HistoryCsv {
    const val HEADER = "date,enter,exit,pause_minutes,worked_minutes,target_minutes"

    private val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun export(history: List<WorkHistoryEntry>, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        append(HEADER).append('\n')
        history.sortedBy { it.enterTime }.forEach { entry ->
            val enter = Instant.ofEpochMilli(entry.enterTime).atZone(zone)
            val exit = Instant.ofEpochMilli(entry.exitTime).atZone(zone)
            val pauseMinutes = TimeUnit.MILLISECONDS.toMinutes(WorkTimeCalculator.totalPauseMillis(entry.pauses))
            val workedMinutes = TimeUnit.MILLISECONDS.toMinutes(
                WorkTimeCalculator.workedMillis(entry.enterTime, entry.exitTime, entry.pauses) ?: 0,
            )
            val targetMinutes = Math.round(entry.dailyHoursTarget * 60)
            append(dateFormat.format(enter)).append(',')
            append(timeFormat.format(enter)).append(',')
            append(timeFormat.format(exit)).append(',')
            append(pauseMinutes).append(',')
            append(workedMinutes).append(',')
            append(targetMinutes).append('\n')
        }
    }
}
