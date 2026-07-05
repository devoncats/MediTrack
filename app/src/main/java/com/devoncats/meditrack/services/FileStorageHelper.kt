package com.devoncats.meditrack.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

class FileStorageHelper(private val context: Context) {

    fun savePhoto(uri: Uri): String {
        val photosDir = File(context.filesDir, MEDICATIONS_DIR).apply { mkdirs() }
        val fileName = "medication_${System.currentTimeMillis()}.jpg"
        val destFile = File(photosDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }

        return "$MEDICATIONS_DIR/$fileName"
    }

    fun loadPhoto(path: String): Bitmap? {
        val file = File(context.filesDir, path)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun deletePhoto(path: String) {
        File(context.filesDir, path).delete()
    }

    companion object {
        private const val MEDICATIONS_DIR = "medications"
    }
}
