package com.vatsalya.founderpocket.ui.capture.forms

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
private val isoFormatter     = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

fun formatDateMillis(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate().format(isoFormatter)

fun displayDate(iso: String): String = runCatching {
    java.time.LocalDate.parse(iso).format(displayFormatter)
}.getOrDefault(iso)

fun dateAtHourMillis(isoDate: String, hour: Int): Long = runCatching {
    java.time.LocalDate.parse(isoDate)
        .atTime(hour, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrDefault(0L)
