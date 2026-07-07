package com.aliadas.resources

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aliadas.R
import com.aliadas.databinding.ItemResourceBinding
import com.aliadas.network.ResourceResponse

class ResourceAdapter(
    private val resources: List<ResourceResponse>,
    private val onAction: (ResourceResponse) -> Unit
) : RecyclerView.Adapter<ResourceAdapter.ResourceViewHolder>() {

    inner class ResourceViewHolder(val binding: ItemResourceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceViewHolder {
        val binding = ItemResourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ResourceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResourceViewHolder, position: Int) {
        val resource = resources[position]
        with(holder.binding) {
            tvTitle.text = resource.title
            tvDescription.text = resource.description
            btnAction.text = resource.actionLabel
            
            chip24h.visibility = if (resource.isAvailable24h) View.VISIBLE else View.GONE
            
            // Mapeo dinámico de iconos según el string recibido
            val iconRes = when (resource.icon) {
                "police" -> R.drawable.ic_local_police
                "woman" -> R.drawable.ic_female
                "medical" -> R.drawable.ic_medical_services
                "emergency" -> R.drawable.ic_emergency
                "psychology" -> R.drawable.ic_psychology
                else -> R.drawable.ic_library_books
            }
            ivIcon.setImageResource(iconRes)

            btnAction.setOnClickListener { onAction(resource) }
        }
    }

    override fun getItemCount(): Int = resources.size
}
