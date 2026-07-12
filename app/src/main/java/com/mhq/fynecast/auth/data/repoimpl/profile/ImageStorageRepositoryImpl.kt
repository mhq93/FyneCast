package com.mhq.fynecast.auth.data.repoimpl.profile

import android.content.Context
import androidx.core.net.toUri
import com.mhq.fynecast.auth.domain.repository.profile.ImageStorageRepository
import com.mhq.fynecast.auth.ui.editprofile.ImageStorageManager

class ImageStorageRepositoryImpl(
    private val context: Context,
    private val imageStorageManager: ImageStorageManager
) : ImageStorageRepository {

    override fun isAlreadySandboxed(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        val uri = uriString.toUri()
        return uri.scheme == "file" && uri.path?.contains(context.filesDir.path) == true
    }

    override suspend fun sandboxAndCompressImage(uriString: String): String? {
        return imageStorageManager.processAndSaveProfileImage(uriString)
    }
}