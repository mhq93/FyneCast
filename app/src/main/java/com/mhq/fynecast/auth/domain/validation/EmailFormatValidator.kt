package com.mhq.fynecast.auth.domain.validation

object EmailFormatValidator {
    private val emailRegex = Regex(
        "^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$"
    )

    /** Strict: an empty string is NOT a valid email. */
    fun isValid(email: String): Boolean = email.matches(emailRegex)

    /** For live typing feedback: nothing is flagged until the user has typed something. */
    fun isValidOrEmpty(email: String): Boolean = email.isEmpty() || isValid(email)
}