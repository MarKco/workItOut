package com.ilsecondodasinistra.workitout.data

import kotlinx.serialization.Serializable

@Serializable
data class SerializablePausePair(
    val start: Long?,
    val end: Long?
)

@Serializable
data class WorkHistoryEntry(
    val id: String,
    val enterTime: Long,
    val pauses: List<SerializablePausePair>,
    val exitTime: Long,
    val totalWorkedTime: String,
    val dailyHoursTarget: Double
)
