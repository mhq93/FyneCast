package com.mhq.fynecast.auth.domain.models

/** Thrown by use cases that validate input themselves;
 * the UI maps [error] to localized text. */
class ValidationException(val error: ValidationError) : Exception()