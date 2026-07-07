package com.aliadas.contacts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("ALIADAS", "Receiver invocado: ${intent.action}")
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val hora = System.currentTimeMillis()
            val prefs = context.getSharedPreferences("ConfiguracionApp", Context.MODE_PRIVATE)
            val guardado = prefs.edit().putLong("ultima_vez", hora).commit()
            android.util.Log.d("ALIADAS", "¡Pantalla desbloqueada! Guardado: $guardado a las $hora")
        }
    }
}
