package com.emoji.app.domain.model

import android.net.Uri
import com.emoji.app.domain.AppConfig

/** Media type enum — determines the processing pipeline */
enum class MediaType { VIDEO, PHOTO }

/** Per-photo text configuration (sparse array element) */
data class PhotoText(
    val content: String = "",
    val fontName: String = AppConfig.DEFAULT_FONT,
    val color: Int = AppConfig.DEFAULT_COLOR,
    val weight: String = "700",
    val x: Float = AppConfig.DEFAULT_TEXT_X,
    val y: Float = AppConfig.DEFAULT_TEXT_Y,
    val size: Int = AppConfig.FONT_SIZE_DEFAULT_PX,
)

/** Frame overflow handling mode */
enum class SampleMode { TRUNCATE, UNIFORM }

/** App step definitions for navigation */
enum class Step(val index: Int) {
    PICK(0),
    TRIM_OR_ORDER(1),
    TEXT_EDIT(2),
    PREVIEW_EXPORT(3)
}
