package com.navitech.cabo.support

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.navitech.cabo.BuildConfig

/** Same inbox as iOS `FeedbackSupport.recipientEmail`. */
object FeedbackSupport {
    const val RECIPIENT_EMAIL = "f.alexandru577@gmail.com"

    fun diagnosticFooter(): String {
        val ver = BuildConfig.VERSION_NAME
        val code = BuildConfig.VERSION_CODE
        val sdk = Build.VERSION.SDK_INT
        val model = Build.MODEL ?: "?"
        return "\n\n—\nVersion $ver ($code)\nAndroid API $sdk, $model"
    }

    /**
     * Opens a mail handler with subject/body prefilled. Returns false if no app handles mailto.
     */
    fun openFeedbackEmail(context: Context): Boolean {
        val subject = "Cabo feedback"
        val body = diagnosticFooter()
        val uri =
            Uri.parse(
                "mailto:$RECIPIENT_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
            )
        val intent =
            Intent(Intent.ACTION_SENDTO).apply {
                data = uri
            }
        return try {
            context.startActivity(Intent.createChooser(intent, "Send feedback"))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
