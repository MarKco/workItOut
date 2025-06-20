package com.ilsecondodasinistra.workitout.data

import kotlinx.serialization.Serializable

@Serializable
data class WorkHistoryEntry(
    val id: String,
    val enterTime: Long,
    val toLunchTime: Long?,
    val fromLunchTime: Long?,
    val exitTime: Long,
    val totalWorkedTime: String,
    val dailyHoursTarget: Double
)
