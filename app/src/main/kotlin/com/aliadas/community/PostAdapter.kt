package com.aliadas.community

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aliadas.R
import com.aliadas.databinding.ItemPostBinding
import com.aliadas.network.PostResponse

class PostAdapter(
    private val posts: List<PostResponse>,
    private val onLike: (PostResponse) -> Unit,
    private val onComment: (PostResponse) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        with(holder.binding) {
            txtPostContent.text = post.content
            txtPostCategory.text = "Categoría: ${post.category.replaceFirstChar { it.uppercase() }}"
            txtLikeCount.text = post.likesCount.toString()
            txtCommentCount.text = "${post.commentsCount} comentarios"
            
            val timeAgo = DateUtils.getRelativeTimeSpanString(
                post.createdAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            txtPostTime.text = timeAgo

            // Visualización del Like
            val likeColor = if (post.hasLiked) R.color.purple_primary else R.color.text_secondary
            
            imgLikeIcon.setImageResource(R.drawable.ic_heart)
            imgLikeIcon.setColorFilter(ContextCompat.getColor(root.context, likeColor))

            btnLike.setOnClickListener { onLike(post) }
            btnComment.setOnClickListener { onComment(post) }
        }
    }

    override fun getItemCount(): Int = posts.size
}
