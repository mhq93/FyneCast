package com.mhq.fynecast.auth.presentation.mapper

import androidx.annotation.StringRes
import com.mhq.fynecast.R
import com.mhq.fynecast.auth.domain.models.ValidationError
import com.mhq.fynecast.auth.domain.models.ValidationException
import com.mhq.fynecast.core.presentation.text.UiText

fun ValidationError.toUiText(): UiText = UiText.res(
    when (this) {
        ValidationError.EmailEmpty -> R.string.error_email_empty
        ValidationError.EmailInvalid -> R.string.error_email_invalid
        ValidationError.PasswordEmpty -> R.string.error_password_empty
        ValidationError.PasswordWeak -> R.string.error_password_weak
        ValidationError.ConfirmPasswordEmpty -> R.string.error_confirm_password_empty
        ValidationError.PasswordsMismatch -> R.string.error_passwords_mismatch
        ValidationError.NameEmpty -> R.string.error_name_empty
        ValidationError.NameInvalidForSignup -> R.string.error_name_invalid_signup
        ValidationError.NameInvalidForProfile -> R.string.error_name_invalid_profile
    }
)

/**
 * Our own typed errors become localized text. Provider errors (Firebase) keep the
 * provider's message, and anything else falls back to [fallbackRes].
 */
fun Throwable.toUiText(@StringRes fallbackRes: Int): UiText =
    (this as? ValidationException)?.error?.toUiText()
        ?: localizedMessage?.takeIf { it.isNotBlank() }?.let { UiText.Dynamic(it) }
        ?: UiText.res(fallbackRes)