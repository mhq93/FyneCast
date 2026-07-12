package com.mhq.fynecast.auth.domain.repository.profile

interface ImageStorageRepository {
    fun isAlreadySandboxed(uriString: String?): Boolean
    suspend fun sandboxAndCompressImage(uriString: String): String?
}