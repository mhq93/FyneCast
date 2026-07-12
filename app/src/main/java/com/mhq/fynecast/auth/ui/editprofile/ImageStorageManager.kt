package com.mhq.fynecast.auth.ui.editprofile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ImageStorageManager(private val context: Context) {

    suspend fun processAndSaveProfileImage(uriString: String?): String? =
        withContext(Dispatchers.IO) {
            if (uriString.isNullOrBlank()) return@withContext null
            val sourceUri = uriString.toUri()

            if (sourceUri.scheme == "file" && sourceUri.path?.contains(context.filesDir.path) == true) {
                return@withContext uriString
            }

            try {
                clearOldProfileImages()

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(sourceUri).use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                val maxDimension = 1080
                var inSampleSize = 1
                if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                    val halfHeight = options.outHeight / 2
                    val halfWidth = options.outWidth / 2
                    while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                        inSampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val sampledBitmap =
                    context.contentResolver.openInputStream(sourceUri).use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    } ?: return@withContext null

                val correctedBitmap = faceCorrectImageOrientation(sourceUri, sampledBitmap)

                val profilePicturesDir = File(context.filesDir, "profile_pics").apply { mkdirs() }
                val uniqueFileName = "profile_${UUID.randomUUID()}.webp"
                val destinationFile = File(profilePicturesDir, uniqueFileName)

                FileOutputStream(destinationFile).use { outStream ->
                    correctedBitmap.compress(Bitmap.CompressFormat.WEBP, 85, outStream)
                }

                if (correctedBitmap != sampledBitmap) {
                    correctedBitmap.recycle()
                }
                sampledBitmap.recycle()

                return@withContext Uri.fromFile(destinationFile).toString()

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }

    /**
     * Purges all previously generated files inside the profile_pics sandbox container directory.
     */
    private fun clearOldProfileImages() {
        try {
            val profilePicturesDir = File(context.filesDir, "profile_pics")
            if (profilePicturesDir.exists() && profilePicturesDir.isDirectory) {
                val files = profilePicturesDir.listFiles()
                files?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun faceCorrectImageOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val exifInterface = inputStream?.use { ExifInterface(it) } ?: return bitmap
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}