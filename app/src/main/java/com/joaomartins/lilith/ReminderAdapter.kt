package com.joaomartins.lilith

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joaomartins.lilith.databinding.ItemReminderBinding

class ReminderAdapter(
    private val onToggle: (Reminder) -> Unit,
    private val onLongClick: (Reminder) -> Unit
) : ListAdapter<Reminder, ReminderAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemReminderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reminder: Reminder) {
            binding.txtReminderTitle.text = reminder.title
            
            val info = if (reminder.intervalMinutes > 0) {
                "A cada ${reminder.intervalMinutes}m (${reminder.startTime} - ${reminder.endTime})"
            } else {
                "Único"
            }
            binding.txtReminderInfo.text = info
            
            binding.switchEnabled.isChecked = reminder.isEnabled
            
            // Se estiver desativado/concluído no ciclo, fica vermelho conforme pedido
            if (reminder.isDismissed) {
                binding.cardReminder.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.RED))
                binding.txtReminderTitle.setTextColor(Color.RED)
            } else {
                binding.cardReminder.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#BB86FC")))
                binding.txtReminderTitle.setTextColor(Color.WHITE)
            }

            binding.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                if (reminder.isEnabled != isChecked) {
                    onToggle(reminder.copy(isEnabled = isChecked, isDismissed = false))
                }
            }

            binding.root.setOnLongClickListener {
                onLongClick(reminder)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Reminder>() {
        override fun areItemsTheSame(oldItem: Reminder, newItem: Reminder) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Reminder, newItem: Reminder) = oldItem == newItem
    }
}
