package com.ilsecondodasinistra.workitout.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: WorkHistoryEntry)

    @Query("SELECT * FROM work_history ORDER BY id DESC")
    fun getAllHistoryFlow(): Flow<List<WorkHistoryEntry>>

    @Query("SELECT * FROM work_history WHERE id = :id")
    suspend fun getHistoryById(id: String): WorkHistoryEntry?

    @Query("DELETE FROM work_history")
    suspend fun clearAllHistory()

    // You can add other queries here if needed, e.g., to delete a specific entry
    @Query("DELETE FROM work_history WHERE id = :id")
    suspend fun deleteById(id: String)
}
