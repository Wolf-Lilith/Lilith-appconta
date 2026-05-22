package com.joaomartins.lilith

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joaomartins.lilith.R
import com.joaomartins.lilith.databinding.ItemAccountBinding
import java.text.SimpleDateFormat
import java.util.*

class AccountAdapter(
    private val onPaidClick: (Account) -> Unit,
    private val onLongClick: (Account) -> Unit // Corrigido de LongClick para onLongClick
) : ListAdapter<Account, AccountAdapter.AccountViewHolder>(AccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AccountViewHolder(private val binding: ItemAccountBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(account: Account) {
            binding.txtDescription.text = account.description

            // Corrigido: Agora pegamos o dia a partir do dueDate (Long)
            val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
            binding.txtDay.text = sdf.format(Date(account.dueDate))

            binding.txtValue.text = String.format("R$ %.2f", account.value)

            val colorRes = if (account.isRevenue) {
                val typedValue = TypedValue()
                binding.root.context.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
                typedValue.resourceId
            } else {
                android.R.color.holo_red_dark
            }

            binding.txtValue.setTextColor(ContextCompat.getColor(binding.root.context, colorRes))

            binding.checkPaid.setOnCheckedChangeListener(null)
            binding.checkPaid.isChecked = account.isPaid

            binding.checkPaid.setOnCheckedChangeListener { _, _ ->
                onPaidClick(account)
            }

            binding.root.setOnLongClickListener {
                onLongClick(account) // Corrigido para usar o nome atualizado
                true
            }
        }
    }

    class AccountDiffCallback : DiffUtil.ItemCallback<Account>() {
        override fun areItemsTheSame(oldItem: Account, newItem: Account) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Account, newItem: Account) = oldItem == newItem
    }
}