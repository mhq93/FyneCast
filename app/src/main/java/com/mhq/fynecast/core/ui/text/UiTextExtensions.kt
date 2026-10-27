package com.mhq.fynecast.core.ui.text

import android.content.Context
import com.mhq.fynecast.core.presentation.text.UiText

/** Turns a [UiText] into a String. Lives in the UI layer because it needs a Context. */
fun UiText.asString(context: Context): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> context.getString(resId, *args.toTypedArray())
}
