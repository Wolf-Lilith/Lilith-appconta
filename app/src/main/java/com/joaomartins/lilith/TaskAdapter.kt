package com.joaomartins.lilith

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joaomartins.lilith.databinding.ItemTaskBinding

class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.txtTaskTitle.text = task.title
            
            // Riscar o texto se concluído
            if (task.isCompleted) {
                binding.txtTaskTitle.paintFlags = binding.txtTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.txtTaskTitle.alpha = 0.5f
            } else {
                binding.txtTaskTitle.paintFlags = binding.txtTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.txtTaskTitle.alpha = 1.0f
            }

            binding.checkTaskCompleted.setOnCheckedChangeListener(null)
            binding.checkTaskCompleted.isChecked = task.isCompleted

            binding.checkTaskCompleted.setOnCheckedChangeListener { _, isChecked ->
                onTaskClick(task.copy(isCompleted = isChecked))
            }

            binding.btnDeleteTask.setOnClickListener {
                onDeleteClick(task)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
    }
}
