package com.android.talkback
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object LogUtils {
    fun createDebugFile(context: Context) {
        if (Build.VERSION.SDK_INT > 29) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appExitInfoList = am.getHistoricalProcessExitReasons(context.packageName, 0, 10)
        val file = File(context.getFilesDir().absolutePath, "/logs.txt")
        if (!file.exists()) {
            file.createNewFile()
        }
        for (exitInfo in appExitInfoList) {
            file.appendText(exitInfo?.toString() ?: "")
        }

        sendFile(file, context)
    }
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
}