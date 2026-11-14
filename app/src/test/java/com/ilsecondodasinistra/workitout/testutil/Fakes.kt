package com.ilsecondodasinistra.workitout.testutil

import com.ilsecondodasinistra.workitout.alarm.ExitReminderScheduler
import com.ilsecondodasinistra.workitout.data.ActiveSession
import com.ilsecondodasinistra.workitout.data.DEFAULT_WORK_DAYS
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class FakeRepository : HistoryRepository {
    override val history = MutableStateFlow<List<WorkHistoryEntry>>(emptyList())
    override val dailyHours = MutableStateFlow(8.0)
    override val workDays = MutableStateFlow<Set<DayOfWeek>>(DEFAULT_WORK_DAYS)
    override val activeSession = MutableStateFlow<ActiveSession?>(null)
    override val dynamicColor = MutableStateFlow(false)
    override suspend fun addEntry(entry: WorkHistoryEntry) {
        history.value = listOf(entry) + history.value.filterNot { it.id == entry.id }
    }
    override suspend fun replaceHistory(entries: List<WorkHistoryEntry>) { history.value = entries }
    override suspend fun clearHistory() { history.value = emptyList() }
    override suspend fun setDailyHours(hours: Double) { dailyHours.value = hours }
    override suspend fun setWorkDays(days: Set<DayOfWeek>) { workDays.value = days }
    override suspend fun saveActiveSession(session: ActiveSession?) { activeSession.value = session }
    override suspend fun setDynamicColor(enabled: Boolean) { dynamicColor.value = enabled }
}

class FakeReminder(var canSchedule: Boolean = true) : ExitReminderScheduler {
    val scheduled = mutableListOf<Long>()
    var cancelCount = 0
    override fun schedule(triggerAtMillis: Long): Boolean {
        scheduled += triggerAtMillis
        return canSchedule
    }
    override fun cancel() { cancelCount++ }
}

class MutableClock(var now: Instant, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
    override fun instant(): Instant = now
}
