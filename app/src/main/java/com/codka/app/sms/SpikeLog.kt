package com.codka.app.sms

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A plain text log of what happened to each Zaad / eDahab SMS (outcome and sender only, never
 * the amount, name or text), in the app's private storage. Started as the task 07 spike; kept
 * for the pilot so a helper can see on a debug build ("Qalabka tijaabada") that payments arrive.
 * Only the last [MAX_LINES] lines are kept, and it never leaves the phone.
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
