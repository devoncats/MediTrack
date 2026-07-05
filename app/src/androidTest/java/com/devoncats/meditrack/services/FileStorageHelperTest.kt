package com.devoncats.meditrack.services

import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileStorageHelperTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fileStorageHelper = FileStorageHelper(context)
    private val savedPaths = mutableListOf<String>()

    @After
    fun cleanUp() {
        savedPaths.forEach { File(context.filesDir, it).delete() }
        File(context.cacheDir, "camera").deleteRecursively()
    }

    private fun sourcePhotoUri(): android.net.Uri {
        val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
        val sourceFile = File(cameraDir, "source_${System.nanoTime()}.jpg")
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        sourceFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sourceFile)
    }

    @Test
    fun savePhoto_thenLoadPhoto_returnsBitmapWithSameDimensions() {
        val path = fileStorageHelper.savePhoto(sourcePhotoUri())
        savedPaths.add(path)

        val loaded = fileStorageHelper.loadPhoto(path)

        assertNotNull("loaded bitmap should not be null right after saving", loaded)
        assertEquals(10, loaded!!.width)
        assertEquals(10, loaded.height)
    }

    @Test
    fun loadPhoto_returnsNull_whenFileDoesNotExist() {
        val loaded = fileStorageHelper.loadPhoto("medications/does_not_exist.jpg")

        assertNull("loadPhoto should safely return null for a missing file", loaded)
    }

    @Test
    fun deletePhoto_removesFile_soLoadPhotoReturnsNullAfterward() {
        val path = fileStorageHelper.savePhoto(sourcePhotoUri())
        assertNotNull(fileStorageHelper.loadPhoto(path))

        fileStorageHelper.deletePhoto(path)

        assertNull("photo should no longer be loadable after deletePhoto", fileStorageHelper.loadPhoto(path))
    }

    @Test
    fun savePhoto_persistsAcrossNewFileStorageHelperInstances() {
        val path = fileStorageHelper.savePhoto(sourcePhotoUri())
        savedPaths.add(path)

        val newSessionHelper = FileStorageHelper(context)

        assertNotNull(
            "photo should still be loadable from a fresh FileStorageHelper instance, proving real disk persistence",
            newSessionHelper.loadPhoto(path)
        )
    }
}
