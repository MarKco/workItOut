package com.ilsecondodasinistra.workitout.data

import kotlinx.serialization.Serializable

/** A pause inside a work session. Either bound can be unset while the user is still filling it in. */
@Serializable
data class PausePair(
    val start: Long? = null,
    val end: Long? = null,
)

/** A completed work day, as stored in the history. Field names are part of the on-disk format. */
@Serializable
data class WorkHistoryEntry(
    val id: String,
    val enterTime: Long,
    val pauses: List<PausePair>,
    val exitTime: Long,
    val totalWorkedTime: String,
    val dailyHoursTarget: Double,
)

/** The work day currently in progress (entered, not yet exited). */
data class ActiveSession(
    val enterTime: Long,
    val pauses: List<PausePair> = emptyList(),
)
