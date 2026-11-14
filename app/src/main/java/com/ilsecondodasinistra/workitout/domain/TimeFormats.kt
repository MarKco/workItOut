package com.ilsecondodasinistra.workitout.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Display formatting for epoch milliseconds, in the device zone and locale. */
object TimeFormats {
    const val PLACEHOLDER = "N/A"

    private fun formatter(pattern: String) = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())

    private fun format(millis: Long?, pattern: String, zone: ZoneId): String =
        millis?.let { formatter(pattern).format(Instant.ofEpochMilli(it).atZone(zone)) } ?: PLACEHOLDER

    fun time(millis: Long?, zone: ZoneId = ZoneId.systemDefault()) = format(millis, "HH:mm:ss", zone)
    fun timeShort(millis: Long?, zone: ZoneId = ZoneId.systemDefault()) = format(millis, "HH:mm", zone)
    fun date(millis: Long?, zone: ZoneId = ZoneId.systemDefault()) = format(millis, "dd/MM/yyyy", zone)
}
