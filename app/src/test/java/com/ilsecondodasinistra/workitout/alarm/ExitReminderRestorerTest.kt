package com.ilsecondodasinistra.workitout.alarm

import com.ilsecondodasinistra.workitout.data.ActiveSession
import com.ilsecondodasinistra.workitout.data.PausePair
import com.ilsecondodasinistra.workitout.testutil.FakeReminder
import com.ilsecondodasinistra.workitout.testutil.FakeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExitReminderRestorerTest {

    private val hour = 3_600_000L
    private val repository = FakeRepository()
    private val reminder = FakeReminder()
    private val restorer = ExitReminderRestorer(repository, reminder)

    @Test
    fun noSession_schedulesNothing() = runBlocking {
        restorer.restore()
        assertTrue(reminder.scheduled.isEmpty())
    }

    @Test
    fun activeSession_schedulesExpectedExitIncludingPauses() = runBlocking {
        repository.activeSession.value = ActiveSession(1_000, listOf(PausePair(2_000, 2_000 + hour)))
        repository.dailyHours.value = 7.5
        restorer.restore()
        assertEquals(listOf(1_000 + (7.5 * hour).toLong() + hour), reminder.scheduled)
    }
}
