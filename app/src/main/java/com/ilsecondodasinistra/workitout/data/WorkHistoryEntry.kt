package com.ilsecondodasinistra.workitout.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_history")
data class WorkHistoryEntry(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // Date string, e.g., "YYYY-MM-DD"

    @ColumnInfo(name = "enter_time")
    val enterTime: Long?,

    @ColumnInfo(name = "to_lunch_time")
    val toLunchTime: Long?,

    @ColumnInfo(name = "from_lunch_time")
    val fromLunchTime: Long?,

    @ColumnInfo(name = "exit_time")
    val exitTime: Long?,

    @ColumnInfo(name = "total_worked_time")
    val totalWorkedTime: String?,

    @ColumnInfo(name = "daily_hours")
    val dailyHours: String?
)
