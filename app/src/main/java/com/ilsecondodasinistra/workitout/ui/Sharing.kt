package com.ilsecondodasinistra.workitout.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Opens the system share sheet with plain text. @return false if nothing can handle it. */
fun Context.shareText(text: String, chooserTitle: String): Boolean {
    val send = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, text).setType("text/plain")
    return startChooser(send, chooserTitle)
}

/** Writes [csv] to the cache and shares it as a file through [FileProvider]. */
suspend fun Context.shareCsvFile(csv: String, fileName: String, chooserTitle: String): Boolean {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName).apply { writeText(csv) }
        FileProvider.getUriForFile(this@shareCsvFile, "$packageName.fileprovider", file)
    }
    val send = Intent(Intent.ACTION_SEND)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .setType("text/csv")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return startChooser(send, chooserTitle)
}

private fun Context.startChooser(send: Intent, title: String): Boolean = try {
    startActivity(Intent.createChooser(send, title))
    true
} catch (_: ActivityNotFoundException) {
    false
}
