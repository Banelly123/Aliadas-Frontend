package com.aliadas.community

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.aliadas.R
import com.aliadas.databinding.FragmentCommunityBinding
import com.aliadas.databinding.ItemPostBinding
import com.aliadas.network.PostRequest
import com.aliadas.network.PostResponse
import com.aliadas.network.RetrofitClient
import com.aliadas.utils.SessionManager
import kotlinx.coroutines.launch

class CommunityFragment : Fragment() {

    private var _binding: FragmentCommunityBinding? = null
    private val binding get() = _binding!!
    private val posts = mutableListOf<PostResponse>()
    private lateinit var adapter: PostAdapter
    private var currentFilter = "recent"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCommunityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PostAdapter(posts,
            onLike = { post -> likePost(post) },
            onComment = { post -> showCommentsDialog(post) }
        )

        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter

        binding.chipRecent.setOnClickListener {
            currentFilter = "recent"
            updateFilterButtonsVisuals(binding.chipRecent)
            loadPosts()
        }
        binding.chipPopular.setOnClickListener {
            currentFilter = "popular"
            updateFilterButtonsVisuals(binding.chipPopular)
            loadPosts()
        }
        binding.chipApoyo.setOnClickListener {
            currentFilter = "apoyo"
            updateFilterButtonsVisuals(binding.chipApoyo)
            loadPosts()
        }

        binding.btnPublish.setOnClickListener { publishPost() }
        binding.swipeRefresh.setOnRefreshListener { loadPosts() }

        binding.imgProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        loadProfileAvatar()
        loadPosts()
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

    private fun loadPosts() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = SessionManager.getBearerToken(requireContext())
                if (token.isEmpty()) return@launch
                
                val res = RetrofitClient.api.getPosts(token, currentFilter)
                if (res.isSuccessful && _binding != null) {
                    posts.clear()
                    posts.addAll(res.body() ?: emptyList())
                    adapter.notifyDataSetChanged()
                    binding.tvEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (_: Exception) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error al cargar publicaciones", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun publishPost() {
        val content = binding.etPost.text.toString().trim()
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor, escribe un testimonio para publicar", Toast.LENGTH_SHORT).show()
            return
        }

        // Mapeo dinámico e inteligente según el Chip Bento seleccionado por la usuaria
        val category = when (binding.chipCategory.checkedChipId) {
            R.id.chipCatApoyo -> "apoyo"
            R.id.chipCatDuda -> "duda"
            R.id.chipCatReporte -> "reporte"
            else -> "general"
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = SessionManager.getBearerToken(requireContext())
                if (token.isEmpty()) return@launch
                
                val res = RetrofitClient.api.createPost(token, PostRequest(content, category))
                if (res.isSuccessful && _binding != null) {
                    binding.etPost.setText("")
                    Toast.makeText(requireContext(), "◈ Publicado anónimamente", Toast.LENGTH_SHORT).show()
                    loadPosts()
                }
            } catch (_: Exception) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error al subir la publicación", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateFilterButtonsVisuals(selectedButton: View) {
        val inactiveTextColor = resources.getColor(R.color.purple_primary, null)
        val inactiveBg = R.drawable.bg_pill_inactive
        
        binding.chipRecent.setBackgroundResource(inactiveBg)
        binding.chipRecent.setTextColor(inactiveTextColor)
        binding.chipPopular.setBackgroundResource(inactiveBg)
        binding.chipPopular.setTextColor(inactiveTextColor)
        binding.chipApoyo.setBackgroundResource(inactiveBg)
        binding.chipApoyo.setTextColor(inactiveTextColor)

        selectedButton.setBackgroundResource(R.drawable.bg_pill_active)
        (selectedButton as? android.widget.Button)?.setTextColor(resources.getColor(R.color.white, null))
    }

    private fun likePost(post: PostResponse) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = SessionManager.getBearerToken(requireContext())
                if (token.isEmpty()) return@launch
                RetrofitClient.api.likePost(token, post.id)
                loadPosts()
            } catch (_: Exception) { }
        }
    }

    private fun showCommentsDialog(post: PostResponse) {
        val dialog = CommentsBottomSheet.newInstance(post.id)
        dialog.show(childFragmentManager, "comments")
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