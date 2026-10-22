package com.mhq.fynecast.auth.data.repoimpl.profile

import android.content.Context
import androidx.core.net.toUri
import com.mhq.fynecast.auth.domain.repository.profile.ImageStorageRepository
import com.mhq.fynecast.auth.data.storage.ImageStorageManager
import java.io.File

class ImageStorageRepositoryImpl(
    private val context: Context,
    private val imageStorageManager: ImageStorageManager
) : ImageStorageRepository {

    override fun isAlreadySandboxed(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val uri = uriString.toUri()
        if (uri.scheme != "file") return false
        val path = uri.path ?: return false

        val canonicalFilesDir = File(context.filesDir.path).canonicalPath + File.separator
        val canonicalTarget = runCatching { File(path).canonicalPath }.getOrNull() ?: return false

        return canonicalTarget.startsWith(canonicalFilesDir)
    }

    override suspend fun sandboxAndCompressImage(uriString: String): String? {
        return imageStorageManager.processAndSaveProfileImage(uriString)
    }

    override fun commitProfileImage(uriString: String) {
        imageStorageManager.commitProfileImage(uriString)
    }

    override fun deleteProfileImage(uriString: String) {
        imageStorageManager.deleteProfileImage(uriString)
    }
}