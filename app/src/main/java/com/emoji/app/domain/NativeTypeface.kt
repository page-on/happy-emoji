package com.emoji.app.domain

import android.graphics.Typeface

/**
 * Creates visually distinct Android Typeface instances using system font family names.
 * Shared between UI preview overlay (Canvas Bitmap) and export renderer.
 */
object NativeTypeface {

    fun forLabel(label: String): Typeface = when (label) {
        "黑体"     -> Typeface.create("sans-serif-medium", Typeface.BOLD)
        "楷体"     -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        "仿宋"     -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        "默认"     -> Typeface.DEFAULT
        "粗黑搞怪" -> Typeface.create("sans-serif-black", Typeface.BOLD)
        "手写涂鸦" -> Typeface.DEFAULT_BOLD.run {
            Typeface.create(this, Typeface.ITALIC)
        }
        "萌萌圆体" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
        "圆体"     -> Typeface.create("sans-serif", Typeface.NORMAL)
        else       -> Typeface.DEFAULT_BOLD
    }

    fun isBold(label: String): Boolean = when (label) {
        "仿宋", "默认", "萌萌圆体", "圆体" -> false
        else -> true
    }
}
