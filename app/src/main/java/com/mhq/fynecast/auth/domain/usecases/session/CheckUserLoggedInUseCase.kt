package com.mhq.fynecast.auth.domain.usecases.session

import com.mhq.fynecast.auth.domain.repository.auth.AuthRepository

class CheckUserLoggedInUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Boolean = authRepository.isUserLoggedIn()
}