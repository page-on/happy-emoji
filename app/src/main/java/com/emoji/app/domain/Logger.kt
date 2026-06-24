package com.emoji.app.domain

import android.util.Log

/**
 * Structured logger following the format:
 *   [大任务|Task] 描述 | key=value key=value
 *
 * 大任务: 视频转GIF / 图片转GIF / System
 */
object EmojiLogger {

    private var isDebug = true

    fun init(debug: Boolean) { isDebug = debug }

    fun info(task: String, subTask: String, msg: String, vararg pairs: Pair<String, Any>) {
        if (!isDebug) return
        log(Log.INFO, task, subTask, msg, *pairs)
    }

    fun warn(task: String, subTask: String, msg: String, vararg pairs: Pair<String, Any>) {
        log(Log.WARN, task, subTask, msg, *pairs)
    }

    fun error(task: String, subTask: String, msg: String, vararg pairs: Pair<String, Any>) {
        log(Log.ERROR, task, subTask, msg, *pairs)
    }

    private fun log(level: Int, task: String, subTask: String, msg: String, vararg pairs: Pair<String, Any>) {
        val params = pairs.joinToString(" ") { "${it.first}=${it.second}" }
        val line = if (params.isNotEmpty()) "$msg | $params" else msg
        val tag = "[$task|$subTask]"
        when (level) {
            Log.INFO -> Log.i(tag, line)
            Log.WARN -> Log.w(tag, line)
            Log.ERROR -> Log.e(tag, line)
        }
    }
}
