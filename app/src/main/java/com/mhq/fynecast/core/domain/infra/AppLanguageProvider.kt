package com.mhq.fynecast.core.domain.infra

import kotlinx.coroutines.flow.Flow

interface AppLanguageProvider {
    /** The app language code ("en", "ar"): the current one first, then again on every change. */
    fun observeLanguageCode(): Flow<String>
}
