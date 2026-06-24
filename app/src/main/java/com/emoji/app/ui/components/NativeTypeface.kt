package com.emoji.app.ui.components

import android.graphics.Typeface

/**
 * UI-level convenience redirect to the domain NativeTypeface.
 */
object NativeTypeface {
    fun forLabel(label: String): Typeface = com.emoji.app.domain.NativeTypeface.forLabel(label)
    fun isBold(label: String): Boolean = com.emoji.app.domain.NativeTypeface.isBold(label)
}
