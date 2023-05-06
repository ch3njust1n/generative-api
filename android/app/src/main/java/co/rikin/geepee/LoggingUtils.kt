package co.rikin.geepee

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class Logger(private val context: Context) {
    fun logToFile(tag: String, message: String) {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logMessage = "$timeStamp - $tag: $message\n"

        try {
            val logDirectory = File(context.filesDir, "Log")
            if (!logDirectory.exists()) {
                logDirectory.mkdirs()
            }

            val logFile = File(logDirectory, "app_log.txt")
            if (!logFile.exists()) {
                logFile.createNewFile()
            }

            val fileOutputStream = FileOutputStream(logFile, true)
            fileOutputStream.write(logMessage.toByteArray())
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
