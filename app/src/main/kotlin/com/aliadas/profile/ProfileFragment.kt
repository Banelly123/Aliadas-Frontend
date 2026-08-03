package com.aliadas.profile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aliadas.R
import com.aliadas.auth.LoginActivity
import com.aliadas.databinding.FragmentProfileBinding
import com.aliadas.network.RetrofitClient
import com.aliadas.network.UpdateProfileRequest
import com.aliadas.utils.SessionManager
import kotlinx.coroutines.launch

// Nombres exactos que espera el servidor (sin prefijos)
val AVATAR_OPTIONS = listOf(
    "cat",
    "bird",
    "butterfly",
    "fox",
    "panda",
    "rabbit"
)

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var currentAvatar = "cat"
    private var isFirstLoad = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnChangeAvatar.setOnClickListener { showAvatarPicker() }
        binding.btnSave.setOnClickListener { saveProfile() }
        binding.btnLogout.setOnClickListener { logout() }

        // Gestión de Ubicación Compartida
        val isSharing = com.aliadas.utils.SessionManager.isLocationSharingEnabled(requireContext())
        binding.switchShareLocation.isChecked = isSharing

        binding.switchShareLocation.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                com.aliadas.contacts.AlertService.stop(requireContext())
                lifecycleScope.launch {
                    com.aliadas.utils.SessionManager.setLocationSharing(requireContext(), false)
                }
                Toast.makeText(requireContext(), "Protocolo de emergencia desactivado", Toast.LENGTH_SHORT).show()
            } else {
                if (!com.aliadas.utils.SessionManager.isLocationSharingEnabled(requireContext())) {
                    binding.switchShareLocation.isChecked = false
                    Toast.makeText(requireContext(), "El protocolo solo se activa con el botón de pánico o llamada de confianza", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadProfile() {
        val cachedAvatar = SessionManager.getAvatar(requireContext()) ?: "cat"
        currentAvatar = cleanAvatarName(cachedAvatar)
        updateAvatarImage(currentAvatar)

        lifecycleScope.launch {
            try {
                val token = SessionManager.getBearerToken(requireContext())
                val res = RetrofitClient.api.getProfile(token)
                if (res.isSuccessful) {
                    val profile = res.body()!!
                    binding.etName.setText(profile.name)
                    binding.tvEmail.text = profile.email

                    if (!profile.avatarIcon.isNullOrEmpty()) {
                        val serverAvatar = cleanAvatarName(profile.avatarIcon!!)
                        // Solo actualizamos si el usuario no ha cambiado el avatar en esta sesión
                        if (isFirstLoad || currentAvatar == cleanAvatarName(cachedAvatar)) {
                            currentAvatar = serverAvatar
                            SessionManager.updateProfile(requireContext(), profile.name, currentAvatar)
                            updateAvatarImage(currentAvatar)
                        }
                    }
                }
            } catch (e: Exception) {
            } finally {
                isFirstLoad = false
            }
        }
    }

    private fun cleanAvatarName(rawName: String): String {
        var name = rawName.lowercase().trim()
        if (name.contains(":")) name = name.split(":")[0]
        if (name.startsWith("avatar_")) name = name.substring(7)
        return if (AVATAR_OPTIONS.contains(name)) name else "cat"
    }

    private fun updateAvatarImage(avatarName: String) {
        try {
            currentAvatar = avatarName
            val colorStr = GeometricAvatarDrawable.getDefaultColorFor(avatarName)
            val color = Color.parseColor(colorStr)
            val drawable = GeometricAvatarDrawable(avatarName, color)
            binding.ivAvatar.setImageDrawable(drawable)
            binding.ivAvatarToolbar.setImageDrawable(GeometricAvatarDrawable(avatarName, color))
        } catch (e: Exception) {
            val defaultColor = Color.parseColor("#FF80AB")
            binding.ivAvatar.setImageDrawable(GeometricAvatarDrawable("avatar_cat", defaultColor))
            binding.ivAvatarToolbar.setImageDrawable(GeometricAvatarDrawable("avatar_cat", defaultColor))
        }
    }

    private fun showAvatarPicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_avatar_picker, null)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.ivPreview)
        val rvAnimals = dialogView.findViewById<RecyclerView>(R.id.rvAnimals)

        dialogView.findViewById<View>(R.id.layoutColors)?.parent?.let { (it as View).visibility = View.GONE }
        dialogView.findViewById<View>(R.id.tvAnimalLabel)?.visibility = View.GONE

        var selectedType = currentAvatar

        fun updatePreview() {
            val colorStr = GeometricAvatarDrawable.getDefaultColorFor(selectedType)
            ivPreview.setImageDrawable(GeometricAvatarDrawable(selectedType, Color.parseColor(colorStr)))
        }

        updatePreview()

        rvAnimals.layoutManager = GridLayoutManager(requireContext(), 3)
        rvAnimals.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val iv = ImageView(requireContext()).apply {
                    val size = (80 * resources.displayMetrics.density).toInt()
                    layoutParams = ViewGroup.LayoutParams(size, size)
                    setPadding(12, 12, 12, 12)
                }
                return object : RecyclerView.ViewHolder(iv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val type = AVATAR_OPTIONS[position]
                val iv = holder.itemView as ImageView
                val colorStr = GeometricAvatarDrawable.getDefaultColorFor(type)
                iv.setImageDrawable(GeometricAvatarDrawable(type, Color.parseColor(colorStr)))
                iv.setBackgroundResource(if (selectedType == type) R.drawable.bg_avatar_selector_selected else 0)
                iv.setOnClickListener {
                    selectedType = type
                    updatePreview()
                    notifyDataSetChanged()
                }
            }
            override fun getItemCount() = AVATAR_OPTIONS.size
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Elige tu avatar")
            .setView(dialogView)
            .setPositiveButton("Seleccionar") { _, _ ->
                currentAvatar = selectedType
                isFirstLoad = false // El usuario ya interactuó
                updateAvatarImage(currentAvatar)
                lifecycleScope.launch {
                    val currentName = binding.etName.text.toString().trim()
                    SessionManager.updateProfile(requireContext(), currentName, currentAvatar)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val token = SessionManager.getBearerToken(requireContext())
                val res = RetrofitClient.api.updateProfile(
                    token,
                    UpdateProfileRequest(name = name, avatarIcon = currentAvatar)
                )
                if (res.isSuccessful) {
                    val savedProfile = RetrofitClient.api.getProfile(token)
                    val savedName = savedProfile.body()?.name ?: name
                    val savedAvatar = savedProfile.body()?.avatarIcon
                        ?.let(::cleanAvatarName)
                        ?: currentAvatar
                    currentAvatar = savedAvatar
                    SessionManager.updateProfile(requireContext(), savedName, savedAvatar)
                    binding.etName.setText(savedName)
                    Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    updateAvatarImage(savedAvatar)
                } else {
                    Toast.makeText(requireContext(), "Error al guardar cambios", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Segura que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                lifecycleScope.launch {
                    SessionManager.clearSession(requireContext())
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
