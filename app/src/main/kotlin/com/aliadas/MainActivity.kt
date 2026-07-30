package com.aliadas

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.aliadas.auth.LoginActivity
import com.aliadas.databinding.ActivityMainBinding
import com.aliadas.utils.LastUnlockManager
import com.aliadas.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            checkBackgroundLocation()
        } else {
            Toast.makeText(this, "Se requieren permisos para tu seguridad", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Pedir permisos críticos de inmediato
        requestInitialPermissions()
        requestIgnoreBatteryOptimizations()
        requestUsageAccessIfNeeded()

        // 2. Verificación de sesión
        val token = SessionManager.getBearerToken(this)
        if (token.isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Sincronización del menú inferior
        binding.bottomNav.setupWithNavController(navController)
        binding.bottomNav.setOnItemSelectedListener { item ->
            // El perfil se abre sobre la pestaña actual. Antes de cambiar de
            // pestaña lo retiramos del historial para que no vuelva a aparecer
            // al regresar a la vista anterior.
            if (navController.currentDestination?.id == R.id.profileFragment) {
                navController.popBackStack()
            }

            NavigationUI.onNavDestinationSelected(item, navController)
        }
    }

    private fun requestInitialPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(toRequest.toTypedArray())
        } else {
            checkBackgroundLocation()
        }
    }

    private fun checkBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showBackgroundLocationDialog()
            }
        }
    }

    private fun showBackgroundLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de Ubicación Todo el Tiempo")
            .setMessage("Para protegerte incluso cuando la app está cerrada, por favor selecciona 'Permitir todo el tiempo' en la siguiente pantalla de ajustes.")
            .setPositiveButton("Configurar") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                }
            }
            .setNegativeButton("Más tarde", null)
            .show()
    }

    private fun requestIgnoreBatteryOptimizations() {
        val packageName = packageName
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun requestUsageAccessIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
            LastUnlockManager.hasUsageAccess(this)
        ) {
            return
        }

        val prefs = getSharedPreferences("ConfiguracionApp", Context.MODE_PRIVATE)
        if (prefs.getBoolean("usage_access_prompted", false)) return
        prefs.edit().putBoolean("usage_access_prompted", true).apply()

        AlertDialog.Builder(this)
            .setTitle("Permitir acceso al uso")
            .setMessage(
                "Aliadas necesita este acceso para incluir en la alerta la hora real " +
                        "del último desbloqueo del teléfono. En la siguiente pantalla, " +
                        "busca Aliadas y activa el permiso."
            )
            .setPositiveButton("Configurar") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "No fue posible abrir la configuración de acceso al uso",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Más tarde", null)
            .show()
    }
}
