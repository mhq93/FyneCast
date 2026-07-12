package com.mhq.fynecast.auth.domain.models

sealed interface ValidationResult {
    object Success : ValidationResult
    data class Failure(val errorMessage: String) : ValidationResult
}