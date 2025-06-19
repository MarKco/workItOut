package com.ilsecondodasinistra.workitout.ui // Or a more appropriate data/repository package

import android.util.Log // Added for logging
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
// Removed UUID import as it's not used in the fix

/**
 * A simple in-memory repository for storing and managing work history records.
 * In a real application, this would typically be replaced by a database solution
 * like Room for persistent storage.
 */
object LocalHistoryRepository {

    private val _history = mutableListOf<Map<String, Any?>>()
    val history: List<Map<String, Any?>>
        get() = _history.toList()

    private val dateFormatForId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun addRecord(record: Map<String, Any?>) {
        val newRecord = record.toMutableMap()
        // Ensure an ID based on the date is present
        if (!newRecord.containsKey("id") || newRecord["id"] == null) {
            // Use enterTime to derive the date for the ID, fallback to current date
            val dateForId = (newRecord["enterTime"] as? Long)?.let { Date(it) } ?: Date()
            newRecord["id"] = dateFormatForId.format(dateForId)
            Log.d("LocalHistoryRepo", "Generated ID for new record: ${newRecord["id"]}")
        } else {
            Log.d("LocalHistoryRepo", "Using existing ID for record: ${newRecord["id"]}")
        }

        val recordId = newRecord["id"]?.toString() // Ensure it's a string for comparison
        val recordEnterTime = newRecord["enterTime"] as? Long

        // Remove existing records with the same date ID AND the same enterTime
        // to prevent duplicate keys in LazyColumn.
        if (recordId != null && recordEnterTime != null) {
            val originalSize = _history.size
            _history.removeAll {
                val existingId = it["id"]?.toString()
                val existingEnterTime = it["enterTime"] as? Long
                existingId == recordId && existingEnterTime == recordEnterTime
            }
            val removedCount = originalSize - _history.size
            if (removedCount > 0) {
                Log.d("LocalHistoryRepo", "Removed $removedCount existing record(s) with same ID ($recordId) and enterTime ($recordEnterTime).")
            }
        } else {
            Log.w("LocalHistoryRepo", "Cannot ensure uniqueness: recordId or recordEnterTime is null. ID: $recordId, EnterTime: $recordEnterTime")
        }

        _history.add(newRecord)
        // Sort by date ID (descending) so newest entries appear first
        // Then by enterTime (ascending) as a secondary sort if needed, though not strictly necessary for the fix
        _history.sortWith(compareByDescending<Map<String, Any?>> { it["id"] as? String ?: "" }
            .thenBy { it["enterTime"] as? Long ?: 0L })
        Log.d("LocalHistoryRepo", "Record added. History size: ${_history.size}. New record details: ID=${newRecord["id"]}, EnterTime=${newRecord["enterTime"]}")
    }

    fun clearHistory() {
        _history.clear()
        Log.d("LocalHistoryRepo", "History cleared.") // Added log
    }

    fun addDummyRecord(enterTimeMs: Long, exitTimeMs: Long, totalWorked: String, dailyHoursSet: Double) {
        val dateForId = Date(enterTimeMs)
        val record = mapOf<String, Any?>(
            "id" to dateFormatForId.format(dateForId),
            "enterTime" to enterTimeMs,
            "toLunchTime" to enterTimeMs + 3600000,
            "fromLunchTime" to enterTimeMs + 7200000,
            "exitTime" to exitTimeMs,
            "totalWorkedTime" to totalWorked,
            "dailyHours" to dailyHoursSet
        )
        addRecord(record) // This will now use the updated addRecord logic
        Log.d("LocalHistoryRepo", "Dummy record added with ID: ${record["id"]}") // Added log
    }
}
