package com.hingefallback

import android.app.Application
import android.os.Handler
import android.os.Looper
import rikka.shizuku.Shizuku

class HingeApp : Application() {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        val publish = Shizuku.OnBinderReceivedListener {
            mainHandler.post { StatusStore.publish() }
        }
        val dead = Shizuku.OnBinderDeadListener {
            mainHandler.post { StatusStore.publish() }
        }
        Shizuku.addBinderReceivedListenerSticky(publish)
        Shizuku.addBinderDeadListener(dead)
    }
}
