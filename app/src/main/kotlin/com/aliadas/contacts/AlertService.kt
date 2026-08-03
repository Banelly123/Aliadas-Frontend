package com.aliadas.contacts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aliadas.R
import com.aliadas.utils.LastUnlockManager
import com.aliadas.utils.SessionManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var alertJob: Job? = null

    companion object {
        private const val TAG = "ALIADAS_ALERT"
        private const val CHANNEL_ID = "aliadas_protection"
        private const val NOTIFICATION_ID = 101
        private const val SEND_INTERVAL_MS = 60_000L
        private const val STATE_PREFERENCES = "aliadas_alert_state"
        private const val KEY_PROTOCOL = "protocol"
        private const val KEY_TARGET_PHONE = "target_phone"
        private const val PROTOCOL_SOS = "sos"
        private const val PROTOCOL_CALL = "call"

        const val EXTRA_TARGET_PHONE = "extra_target_phone"
        const val ACTION_START = "com.aliadas.START_ALARM"

        fun start(context: Context, targetPhone: String? = null) {
            context.getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PROTOCOL, if (targetPhone.isNullOrBlank()) PROTOCOL_SOS else PROTOCOL_CALL)
                .putString(KEY_TARGET_PHONE, targetPhone?.let(::normalizePhone))
                .apply()

            val intent = Intent(context, AlertService::class.java).apply {
                action = ACTION_START
                targetPhone?.let { putExtra(EXTRA_TARGET_PHONE, it) }
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            // stopService funciona aunque la app esté en segundo plano y evita
            // intentar iniciar otro servicio únicamente para detener el actual.
            SessionManager.setLocationSharing(context.applicationContext, false)
            context.getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            context.stopService(Intent(context, AlertService::class.java))
        }

        fun isCallProtocolFor(context: Context, phone: String): Boolean {
            val preferences = context.getSharedPreferences(STATE_PREFERENCES, Context.MODE_PRIVATE)
            val protocol = preferences.getString(KEY_PROTOCOL, null)
            val target = preferences.getString(KEY_TARGET_PHONE, null)
            return protocol == PROTOCOL_CALL &&
                    !target.isNullOrBlank() &&
                    target == normalizePhone(phone)
        }

        private fun normalizePhone(phone: String): String =
            phone.replace(Regex("[^0-9]"), "").takeLast(10)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Android exige esta notificación para conservar el servicio. Su texto
        // es deliberadamente neutral y su contenido se oculta en la pantalla bloqueada.
        startForeground(NOTIFICATION_ID, buildPrivateNotification())

        val targetPhone = intent.getStringExtra(EXTRA_TARGET_PHONE)
        val startedFromSos = targetPhone.isNullOrBlank()
        SessionManager.setLocationSharing(applicationContext, startedFromSos)

        alertJob?.cancel()
        alertJob = serviceScope.launch {
            do {
                sendEmergencyAlert(targetPhone)
                delay(SEND_INTERVAL_MS)
            } while (isActive)
        }

        // Si Android necesita recuperar el proceso, vuelve a entregar el Intent
        // para conservar el destinatario que activó el protocolo.
        return START_REDELIVER_INTENT
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Protección de Aliadas",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mantiene activas las funciones de protección"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildPrivateNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_aliadas_logo_mark)
        .setContentTitle("Aliadas")
        .setContentText("Protección activa")
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        .setSilent(true)
        .setLocalOnly(true)
        .setOngoing(true)
        .build()

    private suspend fun sendEmergencyAlert(targetPhone: String?) {
        val location = getCurrentLocation()
        val message = buildSmsMessage(location?.latitude, location?.longitude)

        if (!targetPhone.isNullOrBlank()) {
            sendSms(targetPhone, message)
            return
        }

        val contactsJson = SessionManager.getTrustedContacts(applicationContext)
        if (contactsJson.isNullOrBlank() || contactsJson == "[]") {
            Log.w(TAG, "No hay contactos de confianza para enviar la alerta")
            return
        }

        runCatching {
            val contacts = JSONArray(contactsJson)
            for (index in 0 until contacts.length()) {
                sendSms(contacts.getJSONObject(index).getString("phone"), message)
            }
        }.onFailure { error ->
            Log.e(TAG, "No fue posible leer los contactos de confianza", error)
        }
    }

    private suspend fun getCurrentLocation(): android.location.Location? {
        val hasFineLocation = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) return null

        val client = LocationServices.getFusedLocationProviderClient(this)
        val current = withTimeoutOrNull(10_000L) {
            runCatching {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            }.getOrNull()
        }
        if (current != null) return current

        return runCatching { client.lastLocation.await() }
            .onFailure { Log.w(TAG, "No fue posible obtener la ubicación", it) }
            .getOrNull()
    }

    private fun buildSmsMessage(lat: Double?, lng: Double?): String {
        val locationText = if (lat != null && lng != null) {
            "Ubicación:\nhttps://maps.google.com/?q=$lat,$lng"
        } else {
            "Ubicación no disponible"
        }
        val battery = (getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val lastUnlock = LastUnlockManager.getLastUnlockTime(this)
        val formattedUnlock = if (lastUnlock == 0L) {
            "No disponible"
        } else {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastUnlock))
        }

        return "ALERTA | ALIADAS\n\nProtocolo de emergencia activo.\n\n" +
                "$locationText\n\nBatería: $battery%\n" +
                "Conexión: ${getNetworkInfo()}\nÚltimo desbloqueo: $formattedUnlock"
    }

    private fun getNetworkInfo(): String {
        val connectivity = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork ?: return "Sin conexión"
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return "Sin conexión"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                val ssid = wifi.connectionInfo?.ssid?.replace("\"", "").orEmpty()
                if (ssid.isBlank() || ssid == "<unknown ssid>") "WiFi" else "WiFi: $ssid"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val telephony = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                "Datos: ${telephony.networkOperatorName}"
            }
            else -> "Conectada"
        }
    }

    private fun sendSms(phone: String, message: String) {
        runCatching {
            val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
            require(cleanPhone.isNotBlank()) { "El teléfono está vacío" }

            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
            Log.i(TAG, "Alerta entregada al sistema de SMS")
        }.onFailure { error ->
            // No incluimos el número en el registro para evitar filtrar datos personales.
            Log.e(TAG, "No fue posible enviar la alerta por SMS", error)
        }
    }

    override fun onDestroy() {
        alertJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
