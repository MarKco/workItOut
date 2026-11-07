package com.ilsecondodasinistra.workitout.domain

import com.ilsecondodasinistra.workitout.data.PausePair
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset

class HistoryCsvAndBackupTest {

    private fun at(s: String) = Instant.parse(s).toEpochMilli()

    private val monday = WorkHistoryEntry(
        id = "1",
        enterTime = at("2026-03-09T09:00:00Z"),
        pauses = listOf(PausePair(at("2026-03-09T12:00:00Z"), at("2026-03-09T13:00:00Z"))),
        exitTime = at("2026-03-09T18:00:00Z"),
        totalWorkedTime = "8h 0m",
        dailyHoursTarget = 7.5,
    )
    private val sunday = monday.copy(
        id = "0",
        enterTime = at("2026-03-08T10:00:00Z"),
        pauses = emptyList(),
        exitTime = at("2026-03-08T12:30:00Z"),
        dailyHoursTarget = 8.0,
    )

    @Test
    fun csv_hasHeaderAndRowsOldestFirst() {
        val csv = HistoryCsv.export(listOf(monday, sunday), ZoneOffset.UTC)
        assertEquals(
            """
            date,enter,exit,pause_minutes,worked_minutes,target_minutes
            2026-03-08,10:00,12:30,0,150,480
            2026-03-09,09:00,18:00,60,480,450
            
            """.trimIndent(),
            csv,
        )
    }

    @Test
    fun csv_emptyHistoryIsHeaderOnly() {
        assertEquals(HistoryCsv.HEADER + "\n", HistoryCsv.export(emptyList(), ZoneOffset.UTC))
    }

    @Test
    fun backup_roundTrips() {
        val text = HistoryBackup.encode(listOf(monday, sunday), 7.5, setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), exportedAt = 42)
        val file = HistoryBackup.decode(text).getOrThrow()
        assertEquals(listOf(monday, sunday), file.history)
        assertEquals(7.5, file.dailyHours, 0.0)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), file.workDaySet())
        assertEquals(42L, file.exportedAt)
    }

    @Test
    fun backup_rejectsGarbage() {
        assertTrue(HistoryBackup.decode("hello").exceptionOrNull() is InvalidBackupException)
        assertTrue(HistoryBackup.decode("[]").exceptionOrNull() is InvalidBackupException)
    }

    @Test
    fun backup_rejectsOtherAppsAndNewerVersions() {
        val ok = HistoryBackup.encode(listOf(monday), 8.0, emptySet(), 1)
        assertTrue(HistoryBackup.decode(ok.replace("\"workitout\"", "\"other\"")).isFailure)
        assertTrue(HistoryBackup.decode(ok.replace("\"version\": 1", "\"version\": 99")).isFailure)
    }

    @Test
    fun backup_rejectsInconsistentData() {
        assertTrue(HistoryBackup.decode(HistoryBackup.encode(listOf(monday.copy(exitTime = monday.enterTime)), 8.0, emptySet(), 1)).isFailure)
        assertTrue(HistoryBackup.decode(HistoryBackup.encode(listOf(monday, monday), 8.0, emptySet(), 1)).isFailure)
        assertTrue(HistoryBackup.decode(HistoryBackup.encode(listOf(monday), 30.0, emptySet(), 1)).isFailure)
    }
}
