package com.example.packetapp.utils

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val outputFormatter: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM yyyy | HH:mm")
        .withLocale(Locale("ru"))
        .withZone(DateTimeZone.forID("Europe/Moscow"))

    fun formatDateTime(isoDateTime: String): String {
        val dateTime = DateTime.parse(isoDateTime)
        return dateTime.toString(outputFormatter)
    }
}