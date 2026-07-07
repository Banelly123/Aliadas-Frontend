package com.aliadas.resources

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliadas.R
import com.aliadas.auth.LoginActivity
import com.aliadas.databinding.FragmentResourcesBinding
import com.aliadas.network.ResourceResponse
import com.aliadas.network.RetrofitClient
import com.aliadas.utils.SessionManager
import kotlinx.coroutines.launch

class ResourcesFragment : Fragment() {

    private var _binding: FragmentResourcesBinding? = null
    private val binding get() = _binding!!
    private val resourcesList = mutableListOf<ResourceResponse>()
    private lateinit var adapter: ResourceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadResources()
    }

    private fun setupRecyclerView() {
        adapter = ResourceAdapter(resourcesList) { resource ->
            if (resource.actionUrl.startsWith("tel:")) {
                triggerCall(resource.actionUrl.substring(4))
            } else {
                openWebPage(resource.actionUrl)
            }
        }
        binding.rvResources.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResources.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.profileImageContainer.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
        binding.swipeRefresh.setOnRefreshListener { loadResources() }
    }

    private fun loadResources() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            try {
                val token = SessionManager.getBearerToken(requireContext())
                if (token.isEmpty()) {
                    _binding?.swipeRefresh?.isRefreshing = false
                    return@launch
                }

                val res = RetrofitClient.api.getResources(token)
                if (res.isSuccessful && _binding != null) {
                    resourcesList.clear()
                    resourcesList.addAll(res.body() ?: emptyList())
                    adapter.notifyDataSetChanged()
                    binding.tvEmpty.visibility = if (resourcesList.isEmpty()) View.VISIBLE else View.GONE
                } else if (_binding != null) {
                    if (res.code() == 401) {
                        Toast.makeText(requireContext(), "Sesión expirada", Toast.LENGTH_SHORT).show()
                        logoutUser()
                    } else {
                        Toast.makeText(requireContext(), "Error al cargar recursos", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error de red", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun logoutUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            SessionManager.clearSession(requireContext())
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun triggerCall(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No se pudo abrir el marcador", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWebPage(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
