package com.mhq.fynecast.core.presentation.text

import androidx.annotation.StringRes

/**
 * Text a ViewModel can publish without touching Context; the UI turns it into a String
 * (see core/ui/text/UiTextExtensions.kt).
 */
sealed class UiText {
    data class Dynamic(val value: String) : UiText()
    data class Resource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText()

    companion object {
        fun res(@StringRes resId: Int, vararg args: Any): UiText = Resource(resId, args.toList())
    }
}