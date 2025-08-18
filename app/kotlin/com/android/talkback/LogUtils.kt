package com.android.talkback
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object LogUtils {
    fun createDebugFile(context: Context, message: String? = null) {
        if (message != null) {
            val file = File(context.getFilesDir().absolutePath, "/debug_messages.txt")
            if (!file.exists()) {
                file.createNewFile()
            }
            file.appendText(message)
            file.appendText("\n")
            return
        }

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val errors = am.getProcessesInErrorState()
        val file = File(context.getFilesDir().absolutePath, "/logs.txt")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeText("")
        if (errors != null) {
        for (error in errors) {
            error ?: continue
            val errorInfo = getErrorInfo(error)
            file.appendText(errorInfo ?: "")
            file.appendText("\n")
        }
        }

        if (Build.VERSION.SDK_INT > 29) {
                    val exitInfoList = am.getHistoricalProcessExitReasons(context.packageName, 0, 0)
            for (exitInfo in exitInfoList) {
                file.appendText(exitInfo.toString())
                file.appendText("\n")
            }
        }
        val debugMessages = File(context.getFilesDir().absolutePath, "/debug_messages.txt")
        if (debugMessages.exists()) {
            file.appendText(debugMessages.readText())
        }

        sendFile(file, context)
    }

    fun sendFile(file: File, context: Context) {
        val intent = Intent(Intent.ACTION_SEND)
        val uri = FileProvider.getUriForFile(
            context, context.packageName + ".provider", file
        )
        intent.setType("text/plain")
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "logs.txt"))
    }

    private fun getErrorInfo(error: ActivityManager.ProcessErrorStateInfo): String {
        val sb = StringBuilder()
        sb.append("condition: ")
        .append(error.condition ?: "")
        .appendLine("Long Message: ")
        .append(error.longMsg ?: "")
        .appendLine("process name: ")
        .append(error.processName)
        .appendLine("Short Message: ")
        .append(error.shortMsg ?: "")
        .appendLine("stack trace: ")
        .append(error.stackTrace ?: "")
        return sb.toString()
    }
}