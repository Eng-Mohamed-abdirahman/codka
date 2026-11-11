package com.codka.app.sms

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Task 07 spike: a plain text log in the app's private storage, so the result can be
 * read on the phone after testing with the app closed. Never leaves the phone.
 */
object SpikeLog {
    private const val FILE_NAME = "spike-log.txt"
    private const val MAX_LINES = 200

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    @Synchronized
    fun add(context: Context, line: String) {
        val time = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())
        val lines = read(context) + "$time  $line"
        file(context).writeText(lines.takeLast(MAX_LINES).joinToString("\n"))
    }

    fun read(context: Context): List<String> =
        file(context).takeIf { it.exists() }?.readLines()?.filter { it.isNotBlank() } ?: emptyList()

    fun clear(context: Context) {
        file(context).delete()
    }
}
