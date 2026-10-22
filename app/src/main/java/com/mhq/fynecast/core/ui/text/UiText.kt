package com.mhq.fynecast.core.ui.text

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Text a ViewModel can publish without touching Context; the UI turns it into a String. */
sealed class UiText {
    data class Dynamic(val value: String) : UiText()
    data class Resource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText()

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> stringResource(resId, *args.toTypedArray())
    }

    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(resId, *args.toTypedArray())
    }

    companion object {
        fun res(@StringRes resId: Int, vararg args: Any): UiText = Resource(resId, args.toList())
    }
}