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

            if (sourceUri.scheme == "file" && isWithinFilesDir(sourceUri.path)) {
                return@withContext uriString
            }

            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(sourceUri).use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                val maxDimension = 1080
                var inSampleSize = 1
                while (
                    options.outHeight / inSampleSize > maxDimension ||
                    options.outWidth / inSampleSize > maxDimension
                ) {
                    inSampleSize *= 2
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

                val compressed = FileOutputStream(destinationFile).use { outStream ->
                    correctedBitmap.compress(Bitmap.CompressFormat.WEBP, 85, outStream)
                }

                if (correctedBitmap != sampledBitmap) {
                    correctedBitmap.recycle()
                }
                sampledBitmap.recycle()

                if (!compressed) {
                    destinationFile.delete()
                    return@withContext null
                }

                return@withContext Uri.fromFile(destinationFile).toString()

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }

    /**
     * Deletes every sandboxed profile image except the one referenced by
     * [keepUriString]. Call this only after the profile record has been
     * durably saved with the new image's URI — deleting the old image any
     * earlier risks leaving the profile pointing at a file that no longer
     * exists if the save fails.
     */
    fun commitProfileImage(keepUriString: String) {
        val keepFileName = keepUriString.toUri().path?.let { File(it).name }
        clearOldProfileImages(excludeFileName = keepFileName)
    }

    /**
     * Deletes a single sandboxed profile image by its file:// URI. Used to
     * roll back a newly-created image when the profile update that was
     * supposed to reference it fails, so it isn't left orphaned.
     */
    fun deleteProfileImage(uriString: String) {
        runCatching {
            uriString.toUri().path?.let { File(it).delete() }
        }
    }

    /**
     * Purges previously generated files inside the profile_pics sandbox container directory,
     * keeping [excludeFileName] (the file that was just written) if provided.
     */
    private fun clearOldProfileImages(excludeFileName: String? = null) {
        try {
            val profilePicturesDir = File(context.filesDir, "profile_pics")
            if (profilePicturesDir.exists() && profilePicturesDir.isDirectory) {
                val files = profilePicturesDir.listFiles()
                files?.forEach { file ->
                    if (file.isFile && file.name != excludeFileName) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isWithinFilesDir(path: String?): Boolean {
        if (path == null) return false
        val canonicalFilesDir = File(context.filesDir.path).canonicalPath + File.separator
        val canonicalTarget = runCatching { File(path).canonicalPath }.getOrNull() ?: return false
        return canonicalTarget.startsWith(canonicalFilesDir)
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