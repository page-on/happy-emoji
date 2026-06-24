package com.emoji.app.domain

/**
 * Global configuration constants for the Emoji GIF Maker app.
 * All tunable parameters live here — business code must never hardcode values.
 */
object AppConfig {
    // ========== 素材限制 ==========
    const val VIDEO_MAX_DURATION_SEC = 30
    const val TRIM_MAX_DURATION_SEC = 10
    const val PHOTO_MIN_COUNT = 1
    const val PHOTO_MAX_COUNT = 20

    // ========== 视频帧提取 ==========
    const val FPS = 8
    const val MAX_FRAMES = 60
    val SPEED_OPTIONS = listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f)
    const val DEFAULT_SPEED = 1.0f

    // ========== 图片帧排序 ==========
    const val FRAME_DELAY_MIN_MS = 500
    const val FRAME_DELAY_MAX_MS = 1500
    const val FRAME_DELAY_DEFAULT_MS = 500
    const val THUMBNAIL_SIZE_DP = 72

    // ========== 文字 ==========
    const val TEXT_MAX_LENGTH = 50
    const val FONT_SIZE_MIN_PX = 16
    const val FONT_SIZE_MAX_PX = 80
    const val FONT_SIZE_DEFAULT_PX = 36
    const val BASE_CANVAS_WIDTH_PX = 420
    val FONT_LIST = listOf(
        "系统默认", "黑体", "楷体", "仿宋", "圆体", "粗黑搞怪", "手写涂鸦", "萌萌圆体"
    )
    val COLOR_PRESETS = listOf(
        0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFFF1744.toInt(),
        0xFFFFD600.toInt(), 0xFF2979FF.toInt(), 0xFF00E676.toInt()
    )
    const val DEFAULT_FONT = "黑体"
    const val DEFAULT_COLOR = 0xFFFFFFFF.toInt()
    const val DEFAULT_TEXT_X = 0.5f
    const val DEFAULT_TEXT_Y = 0.5f

    // ========== 画质 ==========
    enum class Quality(val width: Int, val gifSample: Int, val label: String) {
        LOW(360, 20, "流畅"),
        MEDIUM(480, 10, "标准"),
        HIGH(640, 3, "高清")
    }
    val DEFAULT_QUALITY = Quality.MEDIUM
    const val GIF_SAMPLE_MIN = 1
    const val GIF_SAMPLE_MAX = 30
    const val PHOTO_OUTPUT_WIDTH = 480

    // ========== 导出 ==========
    const val EXPORT_FILENAME_PATTERN = "emoji_yyyyMMdd_HHmmss"
    const val SAVE_DIRECTORY = "Emoji"
    const val GIF_DELAY_DIVISOR = 10
    const val GIF_LOOP_COUNT = 0

    // ========== 性能 ==========
    const val EXPORT_TIMEOUT_VIDEO_SEC = 15
    const val EXPORT_TIMEOUT_PHOTO_SEC = 10
    const val MEMORY_PEAK_MB = 200

    // ========== 缓存 ==========
    const val VIDEO_FRAME_CACHE_MAX = 100
    const val VIDEO_FRAME_CACHE_BACKGROUND = 20
    const val THUMBNAIL_CACHE_MAX = 60
    const val EXPORT_CACHE_MAX_ENTRIES = 3
}
