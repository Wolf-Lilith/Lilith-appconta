package com.joaomartins.lilith

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.joaomartins.lilith.databinding.FragmentTaskListBinding

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private var showCompleted = false

    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory((requireActivity().application as LilithApplication).taskRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TaskAdapter(
            onTaskClick = { viewModel.update(it) },
            onDeleteClick = { viewModel.delete(it) }
        )

        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = adapter

        viewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            updateUI(tasks, adapter)
        }

        binding.btnAddTask.setOnClickListener {
            val title = binding.edtTaskTitle.text.toString()
            if (title.isNotBlank()) {
                viewModel.insert(title)
                binding.edtTaskTitle.text.clear()
                hideKeyboard()
            }
        }

        binding.btnToggleTasksVisibility.setOnClickListener {
            showCompleted = !showCompleted
            updateVisibilityIcon()
            viewModel.allTasks.value?.let { updateUI(it, adapter) }
        }

        updateVisibilityIcon()
    }

    private fun updateUI(tasks: List<Task>, adapter: TaskAdapter) {
        val filteredTasks = if (showCompleted) {
            tasks
        } else {
            tasks.filter { !it.isCompleted }
        }
        adapter.submitList(filteredTasks)
    }

    private fun updateVisibilityIcon() {
        binding.btnToggleTasksVisibility.setImageResource(
            if (showCompleted) android.R.drawable.ic_menu_view else android.R.drawable.ic_menu_close_clear_cancel
        )
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
