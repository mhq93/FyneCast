package com.mhq.fynecast.auth.domain.models

enum class ValidationError {
    EmailEmpty,
    EmailInvalid,
    PasswordEmpty,
    PasswordWeak,
    ConfirmPasswordEmpty,
    PasswordsMismatch,
    NameEmpty,
    NameInvalidForSignup,
    NameInvalidForProfile
}