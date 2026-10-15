package com.mhq.fynecast.auth.domain.validation

object EmailFormatValidator {
    private val emailRegex = Regex("^[a-zA-Z0-9+._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")

    fun isValid(email: String): Boolean {
        return email.isEmpty() || email.matches(emailRegex)
    }
}