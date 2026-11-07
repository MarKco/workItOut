package com.ilsecondodasinistra.workitout.domain

import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import com.ilsecondodasinistra.workitout.ui.StringProvider

/** Human readable history, for the share sheet. */
object HistoryTextExporter {
    fun export(history: List<WorkHistoryEntry>, strings: StringProvider): String = buildString {
        append(strings.get(R.string.work_history)).append(":\n\n")
        history.forEach { entry ->
            append(strings.get(R.string.date, TimeFormats.date(entry.enterTime))).append('\n')
            append("  ").append(strings.get(R.string.entry_time, TimeFormats.time(entry.enterTime))).append('\n')
            entry.pauses.forEach { pause ->
                append("  ").append(strings.get(R.string.pause_start, TimeFormats.time(pause.start))).append('\n')
                append("  ").append(strings.get(R.string.pause_end, TimeFormats.time(pause.end))).append('\n')
            }
            append("  ").append(strings.get(R.string.exit_time, TimeFormats.time(entry.exitTime))).append('\n')
            append("  ").append(strings.get(R.string.total_worked_hours, entry.totalWorkedTime)).append('\n')
            append("  ").append(strings.get(R.string.daily_hours_set, entry.dailyHoursTarget)).append("\n\n")
        }
    }
}
