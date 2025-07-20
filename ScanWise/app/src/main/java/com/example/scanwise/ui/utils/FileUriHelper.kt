package com.example.scanwise.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileUriHelper {
    fun getShareableUri(context: Context, uriString: String): Uri {
        return when {
            uriString.startsWith("content://") -> {
                Uri.parse(uriString)
            }

            uriString.startsWith("file://") -> {
                val file = File(uriString.removePrefix("file://"))
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }

            else -> {
                Uri.parse(uriString)
            }
        }
    }
}