package com.hingefallback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs.isEnabled(context)) return
        ContextCompat.startForegroundService(context, Intent(context, HingeFallbackService::class.java))
    }
}
