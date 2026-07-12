package com.mhq.fynecast.auth.domain.usecases.profile

import com.mhq.fynecast.auth.domain.models.UserDomainModel
import com.mhq.fynecast.auth.domain.repository.profile.UserProfileRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveUserProfileUseCase(
    private val profileRepository: UserProfileRepository
) {
    operator fun invoke(): StateFlow<UserDomainModel> {
        return profileRepository.profileState
    }
}