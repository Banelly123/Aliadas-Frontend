package com.aliadas.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aliadas.databinding.ItemContactBinding
import com.aliadas.network.ContactResponse

class ContactAdapter(
    private val contacts: List<ContactResponse>,
    private val onEdit: (ContactResponse) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        with(holder.binding) {
            tvName.text = contact.name
            tvPhone.text = contact.phone
            tvRelation.text = contact.relation.uppercase()
            
            // Generar iniciales (ej: "Elena García" -> "EG")
            val initials = contact.name.split(" ")
                .filter { it.isNotEmpty() }
                .take(2)
                .map { it[0].uppercase() }
                .joinToString("")
            txtInitials.text = initials

            btnEditContact.setOnClickListener { onEdit(contact) }
        }
    }

    override fun getItemCount(): Int = contacts.size
}
