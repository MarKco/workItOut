package com.ilsecondodasinistra.workitout.ui // Or a more appropriate data/repository package

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * A simple in-memory repository for storing and managing work history records.
 * In a real application, this would typically be replaced by a database solution
 * like Room for persistent storage.
 */
object LocalHistoryRepository {

    // The history is a list of maps, where each map represents a day's record.
    // Keys in the map could be: "id" (String, typically date), "enterTime" (Long),
    // "toLunchTime" (Long), "fromLunchTime" (Long), "exitTime" (Long),
    // "totalWorkedTime" (String), "dailyHours" (Double)
    private val _history = mutableListOf<Map<String, Any?>>()
    val history: List<Map<String, Any?>>
        get() = _history.toList() // Expose an immutable copy

    // Helper to format Date to a YYYY-MM-DD string for use as an ID
    private val dateFormatForId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Adds a new record to the history.
     * The record should be a map containing the day's details.
     * It automatically assigns a date-based ID if not present.
     */
    fun addRecord(record: Map<String, Any?>) {
        val newRecord = record.toMutableMap()
        if (!newRecord.containsKey("id")) {
            // Use enterTime to derive the date for the ID, fallback to current date
            val dateForId = (newRecord["enterTime"] as? Long)?.let { Date(it) } ?: Date()
            newRecord["id"] = dateFormatForId.format(dateForId)
        }
        // To prevent exact duplicate entries based on ID (date), you might want to remove existing first
        // Or, allow multiple entries for the same date if that's a valid scenario (e.g., manual corrections)
        // For simplicity here, we'll just add. If you want to replace by date ID:
        // _history.removeAll { it["id"] == newRecord["id"] }
        _history.add(newRecord)
        // Sort by date ID (descending) so newest entries appear first
        _history.sortByDescending { it["id"] as? String }
    }

    /**
     * Clears all records from the history.
     */
    fun clearHistory() {
        _history.clear()
    }

    /**
     * (For Preview/Testing) Adds a dummy record to the history.
     */
    fun addDummyRecord(enterTimeMs: Long, exitTimeMs: Long, totalWorked: String, dailyHoursSet: Double) {
        val dateForId = Date(enterTimeMs)
        val record = mapOf<String, Any?>(
            "id" to dateFormatForId.format(dateForId),
            "enterTime" to enterTimeMs,
            "toLunchTime" to enterTimeMs + 3600000, // Dummy: 1 hour after entry
            "fromLunchTime" to enterTimeMs + 7200000, // Dummy: 2 hours after entry
            "exitTime" to exitTimeMs,
            "totalWorkedTime" to totalWorked,
            "dailyHours" to dailyHoursSet
        )
        addRecord(record)
    }
}