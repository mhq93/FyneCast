package com.mhq.fynecast.home.domain.usecases

import com.mhq.fynecast.core.domain.infra.CurrentUserProvider
import kotlinx.coroutines.flow.Flow

class ObserveSignedInUserIdUseCase(private val currentUserProvider: CurrentUserProvider) {
    operator fun invoke(): Flow<String?> = currentUserProvider.observeUserId()
}