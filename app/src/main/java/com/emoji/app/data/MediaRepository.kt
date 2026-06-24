package com.emoji.app.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.emoji.app.domain.AppConfig
import com.emoji.app.domain.EmojiLogger
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MediaRepository — handles saving GIFs to system gallery via MediaStore.
 */
object MediaRepository {

    private const val MIME_GIF = "image/gif"

    fun saveGifToGallery(context: Context, gifData: ByteArray): Uri? {
        val filename = generateFilename()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(context, gifData, filename)
            } else {
                @Suppress("DEPRECATION")
                saveViaFile(context, gifData, filename)
            }
        } catch (e: Exception) {
            EmojiLogger.error("System", "StorageCheck", "保存失败", "error" to (e.message ?: "unknown"))
            null
        }
    }

    private fun saveViaMediaStore(context: Context, data: ByteArray, filename: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, MIME_GIF)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/${AppConfig.SAVE_DIRECTORY}")
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        ) ?: return null

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(data)
            out.flush()
        }
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveViaFile(context: Context, data: ByteArray, filename: String): Uri? {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            AppConfig.SAVE_DIRECTORY
        )
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, filename)
        FileOutputStream(file).use { it.write(data) }
        // Notify media scanner
        MediaStore.Images.Media.insertImage(
            context.contentResolver, file.absolutePath, filename, null
        )
        return Uri.fromFile(file)
    }

    fun checkAvailableSpace(requiredBytes: Long): Boolean {
        val free = Environment.getDataDirectory().freeSpace
        return free > requiredBytes
    }

    private fun generateFilename(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "emoji_${sdf.format(Date())}.gif"
    }
}
