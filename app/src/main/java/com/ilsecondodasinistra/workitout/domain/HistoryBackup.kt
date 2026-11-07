package com.ilsecondodasinistra.workitout.domain

import com.ilsecondodasinistra.workitout.data.WorkHistoryEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.DayOfWeek

@Serializable
data class BackupFile(
    val app: String = BACKUP_APP_ID,
    val version: Int = BACKUP_VERSION,
    val exportedAt: Long,
    val dailyHours: Double,
    val workDays: List<String>,
    val history: List<WorkHistoryEntry>,
) {
    fun workDaySet(): Set<DayOfWeek> = workDays.mapNotNull { name -> DayOfWeek.entries.firstOrNull { it.name == name } }.toSet()
}

const val BACKUP_APP_ID = "workitout"
const val BACKUP_VERSION = 1

class InvalidBackupException(message: String) : Exception(message)

/** JSON backup of the whole app state (history and settings). */
object HistoryBackup {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true // the format marker and version must always be written
        ignoreUnknownKeys = true
    }

    fun encode(history: List<WorkHistoryEntry>, dailyHours: Double, workDays: Set<DayOfWeek>, exportedAt: Long): String =
        json.encodeToString(
            BackupFile.serializer(),
            BackupFile(
                exportedAt = exportedAt,
                dailyHours = dailyHours,
                workDays = workDays.sorted().map { it.name },
                history = history,
            ),
        )

    /** Fails with [InvalidBackupException] for anything that is not a usable backup from this app. */
    fun decode(text: String): Result<BackupFile> = runCatching {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw InvalidBackupException("Not a WorkItOut backup: ${e.message}")
        }
        if (file.app != BACKUP_APP_ID) throw InvalidBackupException("Unknown app: ${file.app}")
        if (file.version > BACKUP_VERSION) throw InvalidBackupException("Backup is from a newer version")
        if (file.dailyHours !in 0.0..24.0) throw InvalidBackupException("Invalid daily hours")
        if (file.history.any { it.exitTime <= it.enterTime }) throw InvalidBackupException("Invalid history entry")
        if (file.history.map { it.id }.toSet().size != file.history.size) throw InvalidBackupException("Duplicate ids")
        file
    }
}
