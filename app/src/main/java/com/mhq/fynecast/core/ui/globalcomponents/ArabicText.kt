package com.mhq.fynecast.core.ui.globalcomponents

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Arabic letters join, and any letter spacing breaks the joins, so Arabic gets none. */
@Composable
fun TextUnit.unlessArabic(): TextUnit =
    if (currentLocale().language == "ar") 0.sp else this