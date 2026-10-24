package com.mhq.fynecast.auth.domain.validation

object PasswordValidator {
    private val regex =
        Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$")

    fun isValid(password: String): Boolean = password.matches(regex)
    fun isValidOrEmpty(password: String): Boolean = password.isEmpty() || isValid(password)
}