package com.ilsecondodasinistra.workitout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.workitout.data.HistoryRepository
import com.ilsecondodasinistra.workitout.domain.Stats
import com.ilsecondodasinistra.workitout.domain.WorkStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

class StatsViewModel(
    repository: HistoryRepository,
    clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    /** Null until the first emission. */
    val stats: StateFlow<Stats?> = combine(repository.history, repository.workDays) { history, workDays ->
        WorkStats.compute(history, workDays, LocalDate.now(clock), clock.zone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
