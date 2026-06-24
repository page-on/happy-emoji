package com.emoji.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.emoji.app.domain.EmojiLogger

/**
 * Permission helper — handles READ_EXTERNAL_STORAGE for API 29-32.
 * API 33+ uses PhotoPicker (no permission needed).
 */
object PermissionHelper {

    fun needsStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT in 29..32
    }

    fun hasStoragePermission(context: Context): Boolean {
        if (!needsStoragePermission()) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun logPermissionResult(permission: String, granted: Boolean) {
        EmojiLogger.info(
            "System", "Permission",
            "权限请求",
            "permission" to permission,
            "result" to (if (granted) "GRANTED" else "DENIED")
        )
    }
}
