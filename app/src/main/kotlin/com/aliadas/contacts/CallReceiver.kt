package com.aliadas.contacts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.os.Handler
import android.os.Looper

class CallReceiver : BroadcastReceiver() {

    companion object {
        private var isRinging = false
        private var incomingNumber: String? = null
        private val handler = Handler(Looper.getMainLooper())
        private var alertRunnable: Runnable? = null
        private const val RING_TIMEOUT = 10_000L // 10 segundos
    }

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        var number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        
        // Si el número viene nulo, intentamos sacarlo de otros extras comunes
        if (number == null) {
            number = intent.extras?.getString("incoming_number")
        }

        android.util.Log.d("ALIADAS_CALL", "Llamada recibida. Estado: $state, Número: $number")

        val pendingResult = goAsync()

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                if (number != null && isTrusted(context, number)) {
                    isRinging = true
                    incomingNumber = number
                    
                    alertRunnable = Runnable {
                        if (isRinging) {
                            AlertService.start(context, incomingNumber)
                        }
                        pendingResult.finish()
                    }
                    handler.postDelayed(alertRunnable!!, RING_TIMEOUT)
                } else {
                    pendingResult.finish()
                }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Se contestó una llamada entrante O se inició una saliente
                cancelTimer()
                
                // Si es una llamada saliente (OFFHOOK sin número entrante previo), 
                // el sistema a veces no da el número aquí por privacidad.
                // Pero si el protocolo de alerta está activo y la usuaria descuelga para llamar, 
                // asumimos que está retomando el control.
                AlertService.stop(context)
                pendingResult.finish()
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                isRinging = false
                incomingNumber = null
                // No llamamos a cancelTimer aquí para dejar que el runnable de 10s decida si fue perdida larga
                handler.postDelayed({ try { pendingResult.finish() } catch(e: Exception) {} }, 2000)
            }
            
            else -> {
                if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
                    val outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                    if (outgoingNumber != null && isTrusted(context, outgoingNumber)) {
                        AlertService.stop(context)
                    }
                }
                pendingResult.finish()
            }
        }
    }

    private fun cancelTimer() {
        alertRunnable?.let { handler.removeCallbacks(it) }
        alertRunnable = null
        isRinging = false
    }

    private fun isTrusted(context: Context, number: String): Boolean {
        val prefs = context.getSharedPreferences("aliadas_contacts", Context.MODE_PRIVATE)
        val trustedPhones = prefs.getStringSet("trusted_phones", emptySet()) ?: emptySet()
        val cleanNumber = number.replace(Regex("[^0-9]"), "").takeLast(10)
        return trustedPhones.any { it.replace(Regex("[^0-9]"), "").takeLast(10) == cleanNumber }
    }
}
