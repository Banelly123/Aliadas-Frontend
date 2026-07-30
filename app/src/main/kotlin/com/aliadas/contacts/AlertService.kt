package com.aliadas.contacts

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.aliadas.network.RetrofitClient
import com.aliadas.utils.LastUnlockManager
import com.aliadas.utils.SessionManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import org.json.JSONArray
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AlertService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        const val EXTRA_TARGET_PHONE = "extra_target_phone"
        const val ACTION_START = "com.aliadas.START_ALARM"
        const val ACTION_STOP = "com.aliadas.STOP_ALARM"

        fun start(context: Context, targetPhone: String? = null) {
            val intent = Intent(context, AlertService::class.java).apply {
                action = ACTION_START
                if (targetPhone != null) putExtra(EXTRA_TARGET_PHONE, targetPhone)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AlertService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val targetPhone = intent?.getStringExtra(EXTRA_TARGET_PHONE)

        if (action == ACTION_STOP) {
            Log.d("ALIADAS_ALERT", "Deteniendo protocolo de emergencia...")
            cancelAlarm()
            SessionManager.setLocationSharing(applicationContext, false)
            stopSelf()
            return START_NOT_STICKY
        }

        serviceScope.launch {
            if (action == ACTION_START) {
                SessionManager.setLocationSharing(applicationContext, true)
            }
            sendEmergencyAlert(targetPhone)
            scheduleNextAlarm(targetPhone)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun scheduleNextAlarm(targetPhone: String?) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlertService::class.java).apply {
            action = ACTION_START
            if (targetPhone != null) putExtra(EXTRA_TARGET_PHONE, targetPhone)
        }
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60_000,
            pendingIntent
        )
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlertService::class.java).apply { action = ACTION_START }
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private suspend fun sendEmergencyAlert(targetPhone: String?) {
        val context = applicationContext
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        var lat: Double? = null
        var lng: Double? = null

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val location = withContext(Dispatchers.Main) {
                    suspendCoroutine { cont ->
                        fusedClient.lastLocation.addOnSuccessListener { cont.resume(it) }.addOnFailureListener { cont.resume(null) }
                    }
                }
                lat = location?.latitude
                lng = location?.longitude
            } catch (e: Exception) { }
        }

        val message = buildSmsMessage(context, lat, lng)

        if (targetPhone != null) {
            sendSms(targetPhone, message)
        } else {
            val contactsJson = SessionManager.getTrustedContacts(context)
            if (!contactsJson.isNullOrBlank() && contactsJson != "[]") {
                try {
                    val array = JSONArray(contactsJson)
                    for (i in 0 until array.length()) {
                        sendSms(array.getJSONObject(i).getString("phone"), message)
                    }
                } catch (e: Exception) { }
            }
        }
    }

    private fun buildSmsMessage(context: Context, lat: Double?, lng: Double?): String {
        val loc = if (lat != null && lng != null) "📍 Ubicación: https://maps.google.com/?q=$lat,$lng" else "📍 Ubicación no disponible"
        val battery = (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val networkInfo = getNetworkInfo(context)

        val ultimaVez = LastUnlockManager.getLastUnlockTime(context)

        val horaFormateada = if (ultimaVez == 0L) "No disponible" else
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(ultimaVez))

        return "🚨 ALERTA - Aliadas\n$loc\n🔋 Batería: $battery%\n📶 Red: $networkInfo\n🔓 Último desbloqueo: $horaFormateada"
    }

    private fun getNetworkInfo(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "Sin conexión"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "Sin conexión"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                val ssid = wifiManager.connectionInfo?.ssid?.replace("\"", "") ?: ""
                if (ssid.isEmpty() || ssid == "<unknown ssid>") "WiFi" else "WiFi: $ssid"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                "Datos: ${tm.networkOperatorName}"
            }
            else -> "Conectada"
        }
    }

    private fun sendSms(phone: String, message: String) {
        try {
            val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) getSystemService(SmsManager::class.java) else SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
        } catch (e: Exception) { }
    }

    override fun onBind(intent: Intent?) = null
}
