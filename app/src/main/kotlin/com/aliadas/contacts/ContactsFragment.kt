package com.aliadas.contacts

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliadas.R
import com.aliadas.databinding.FragmentContactsBinding
import com.aliadas.databinding.ItemContactBinding
import com.aliadas.databinding.DialogAddContactBinding
import com.aliadas.network.ContactRequest
import com.aliadas.network.ContactResponse
import com.aliadas.network.RetrofitClient
import com.aliadas.utils.SessionManager
import kotlinx.coroutines.launch

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    private val contacts = mutableListOf<ContactResponse>()
    private lateinit var adapter: ContactAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ContactAdapter(contacts) { contact ->
            showManageContactOptions(contact)
        }

        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.adapter = adapter

        binding.imgProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        loadProfileAvatar()
        binding.fabAdd.setOnClickListener { showAddContactDialog() }
        binding.swipeRefresh.setOnRefreshListener { loadContacts() }

        loadContacts()
    }

    private fun loadProfileAvatar() {
        val avatarData = SessionManager.getAvatar(requireContext()) ?: "cat"
        try {
            val type = if (avatarData.contains(":")) avatarData.split(":")[0] else avatarData
            val colorStr = com.aliadas.profile.GeometricAvatarDrawable.getDefaultColorFor(type)
            val color = android.graphics.Color.parseColor(colorStr)
            val drawable = com.aliadas.profile.GeometricAvatarDrawable(type, color)
            binding.imgProfile.setImageDrawable(drawable)
        } catch (e: Exception) {
            val defaultColor = android.graphics.Color.parseColor("#FF80AB")
            binding.imgProfile.setImageDrawable(com.aliadas.profile.GeometricAvatarDrawable("cat", defaultColor))
        }
    }

    private fun loadContacts() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            try {
                val token = SessionManager.getBearerToken(requireContext())
                if (token.isEmpty()) {
                    _binding?.swipeRefresh?.isRefreshing = false
                    return@launch
                }
                
                val res = RetrofitClient.api.getContacts(token)
                if (res.isSuccessful && _binding != null) {
                    contacts.clear()
                    contacts.addAll(res.body() ?: emptyList())
                    adapter.notifyDataSetChanged()
                    updateTrustedContactsCache(contacts)
                    binding.tvEmpty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
                } else if (_binding != null) {
                    Toast.makeText(requireContext(), "Error al cargar contactos: ${res.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ALIADAS_DEBUG", "Error loading contacts", e)
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error de red al obtener contactos", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun showAddContactDialog() {
        if (contacts.size >= 5) {
            Toast.makeText(requireContext(), "Máximo 5 contactos de confianza", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogAddContactBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Agregar contacto de confianza")
            .setView(dialogBinding.root)
            .setPositiveButton("Agregar", null) // Ponemos null para anular el cierre automático
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val name = dialogBinding.etName.text.toString().trim()
                val phone = dialogBinding.etPhone.text.toString().trim()
                val relation = dialogBinding.etRelation.text.toString().trim().ifBlank { "Contacto" }

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    addContact(name, phone, relation)
                    dialog.dismiss() // Se cierra únicamente si pasa la validación
                } else {
                    Toast.makeText(requireContext(), "Nombre y teléfono son requeridos", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun addContact(name: String, phone: String, relation: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = SessionManager.getBearerToken(requireContext())
                if (token.isEmpty()) {
                    Toast.makeText(requireContext(), "Error: Sesión no válida", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val res = RetrofitClient.api.addContact(token, ContactRequest(name, phone, relation))
                if (res.isSuccessful && _binding != null) {
                    Toast.makeText(requireContext(), "◈ Contacto guardado con éxito", Toast.LENGTH_SHORT).show()
                    loadContacts()
                } else if (_binding != null) {
                    val errorBody = res.errorBody()?.string()
                    android.util.Log.e("ALIADAS_DEBUG", "Error adding contact: $errorBody")
                    Toast.makeText(requireContext(), "Error del servidor: ${res.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ALIADAS_DEBUG", "Exception adding contact", e)
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error de red: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showManageContactOptions(contact: ContactResponse) {
        val options = arrayOf("Eliminar contacto", "Cancelar")
        AlertDialog.Builder(requireContext())
            .setTitle("Gestionar a ${contact.name}")
            .setItems(options) { dialog, which ->
                if (which == 0) deleteContact(contact)
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteContact(contact: ContactResponse) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = SessionManager.getBearerToken(requireContext())
                if (token.isEmpty()) return@launch
                val res = RetrofitClient.api.deleteContact(token, contact.id)
                if (res.isSuccessful && _binding != null) {
                    loadContacts()
                    Toast.makeText(requireContext(), "Contacto eliminado con éxito", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error al eliminar el contacto", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateTrustedContactsCache(contacts: List<ContactResponse>) {
        val prefs = requireContext().getSharedPreferences("aliadas_contacts", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("trusted_phones", contacts.map { it.phone }.toSet()).apply()
        
        // También guardar en SessionManager como JSON para el Service
        viewLifecycleOwner.lifecycleScope.launch {
            val jsonArray = org.json.JSONArray()
            contacts.forEach { 
                val obj = org.json.JSONObject()
                obj.put("phone", it.phone)
                jsonArray.put(obj)
            }
            SessionManager.saveTrustedContacts(requireContext(), jsonArray.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfileAvatar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}