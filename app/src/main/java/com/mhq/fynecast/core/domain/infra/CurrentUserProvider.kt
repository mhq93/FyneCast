package com.mhq.fynecast.core.domain.infra

import kotlinx.coroutines.flow.Flow

interface CurrentUserProvider {
    /** The signed-in user's ID, or null when signed out.
     * Emits again on every sign-in or sign-out. */
    fun observeUserId(): Flow<String?>
    fun currentUserId(): String?
}