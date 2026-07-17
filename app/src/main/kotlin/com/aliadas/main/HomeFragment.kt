package com.aliadas.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aliadas.R
import com.aliadas.databinding.FragmentHomeBinding
import com.aliadas.utils.SessionManager
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.tileprovider.tilesource.TileSourceFactory

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userName = SessionManager.getUserName(requireContext()) ?: "Banelly"
        binding.tvWelcome.text = "Hola, $userName 👋"

        // Inicializar OpenStreetMap
        Configuration.getInstance().load(requireContext(), android.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()))
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)

        // Colocar zoom inicial intermedio
        binding.mapView.controller.setZoom(16.5)

        // 📡 OBTENER UBICACIÓN REAL ACTUAL
        checkLocationPermissions()

        binding.btnPanic.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                com.aliadas.contacts.AlertService.start(requireContext())
                Toast.makeText(requireContext(), "🚨 ¡Protocolo de emergencia activado!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Primero concede permisos de ubicación", Toast.LENGTH_SHORT).show()
                checkLocationPermissions()
            }
        }

        binding.profileImageContainer.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getDeviceLocation()
        } else {
            // Solicitar permisos en pantalla
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1001)
        }
    }

    private fun getDeviceLocation() {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // Convertimos la ubicación real a un punto GPS para OpenStreetMap
                    val currentPoint = GeoPoint(location.latitude, location.longitude)
                    binding.mapView.controller.setCenter(currentPoint)
                } else {
                    // Si el GPS está apagado o tardado, dejamos un punto de respaldo amigable
                    binding.mapView.controller.setCenter(GeoPoint(19.4326, -99.1332))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getDeviceLocation()
        }
    }

    override fun onResume() { super.onResume(); try { binding.mapView.onResume() } catch(e: Exception){} }
    override fun onPause() { super.onPause(); try { binding.mapView.onPause() } catch(e: Exception){} }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
