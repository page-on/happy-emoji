package com.emoji.app

import android.app.Application
import com.emoji.app.domain.EmojiLogger

class EmojiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        EmojiLogger.init(BuildConfig.ENABLE_LOGGING)
        EmojiLogger.info("System", "AppColdStart", "冷启动",
            "appVersion" to BuildConfig.VERSION_NAME,
            "platform" to "Android",
            "deviceModel" to android.os.Build.MODEL
        )
    }

    companion object {
        lateinit var instance: EmojiApplication
            private set
    }
}
