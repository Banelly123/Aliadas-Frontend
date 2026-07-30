package com.aliadas.contacts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aliadas.utils.LastUnlockManager

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            LastUnlockManager.recordUnlock(context)
        }
    }
}
