package com.ilsecondodasinistra.workitout.domain

import com.ilsecondodasinistra.workitout.data.PausePair
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Pure time arithmetic on epoch milliseconds. No Android, no clock: easy to unit test. */
object WorkTimeCalculator {

    private const val MS_PER_HOUR = 3_600_000.0

    /** Length of a pause, or 0 if it is incomplete or ends before it starts. */
    fun pauseMillis(pause: PausePair): Long {
        val start = pause.start ?: return 0
        val end = pause.end ?: return 0
        return if (end > start) end - start else 0
    }

    fun totalPauseMillis(pauses: List<PausePair>): Long = pauses.sumOf(::pauseMillis)

    /** Whole minutes of a pause, or null when it is incomplete or shorter than a minute. */
    fun pauseMinutes(pause: PausePair): Long? =
        TimeUnit.MILLISECONDS.toMinutes(pauseMillis(pause)).takeIf { it > 0 }

    /** When the target hours are done: enter + target + the time spent in pauses. */
    fun expectedExit(enterTime: Long, pauses: List<PausePair>, dailyHours: Double): Long? {
        if (dailyHours <= 0) return null
        return enterTime + (dailyHours * MS_PER_HOUR).toLong() + totalPauseMillis(pauses)
    }

    /** Worked time between enter and exit minus pauses, or null if exit is not after enter. */
    fun workedMillis(enterTime: Long, exitTime: Long, pauses: List<PausePair>): Long? {
        if (exitTime <= enterTime) return null
        return (exitTime - enterTime - totalPauseMillis(pauses)).coerceAtLeast(0)
    }

    /**
     * Time worked from [enterTime] up to [now], minus pauses. A pause still open (started, not ended)
     * counts as paused until [now], so the total stands still while the user is on a break.
     */
    fun workedSoFarMillis(enterTime: Long, now: Long, pauses: List<PausePair>): Long {
        if (now <= enterTime) return 0
        val paused = pauses.sumOf { pause ->
            val start = pause.start ?: return@sumOf 0L
            val end = pause.end ?: now
            if (end > start) end - start else 0L
        }
        return (now - enterTime - paused).coerceAtLeast(0)
    }

    /** A session whose entry is from an earlier day than [now]: the user forgot to record the exit. */
    fun isStale(enterTime: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        Instant.ofEpochMilli(enterTime).atZone(zone).toLocalDate() < Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

    /** "7h 30m" */
    fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return "${hours}h ${minutes}m"
    }

    /** "+1h 30m" / "-45m" / "0m": signed difference, used for overtime balances. */
    fun formatBalance(millis: Long): String {
        val abs = kotlin.math.abs(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(abs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(abs) % 60
        val body = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        return when {
            hours == 0L && minutes == 0L -> body
            millis > 0 -> "+$body"
            else -> "-$body"
        }
    }

    /**
     * Times picked on a clock face have no date. If [candidate] falls before [reference]
     * the user means the next day (e.g. exit at 01:00 after entering at 22:00).
     */
    fun rollToNextDayIfBefore(candidate: Long, reference: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        if (candidate < reference) {
            Instant.ofEpochMilli(candidate).atZone(zone).plusDays(1).toInstant().toEpochMilli()
        } else {
            candidate
        }
}
