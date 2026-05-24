package com.joaomartins.lilith

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.joaomartins.lilith.databinding.FragmentReminderListBinding
import java.util.Calendar

class ReminderListFragment : Fragment() {

    private var _binding: FragmentReminderListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReminderViewModel by viewModels {
        val app = requireActivity().application as LilithApplication
        ReminderViewModelFactory(app, app.reminderRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReminderListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ReminderAdapter(
            onToggle = { viewModel.update(it) },
            onLongClick = { showDeleteDialog(it) }
        )

        binding.rvReminders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReminders.adapter = adapter

        viewModel.allReminders.observe(viewLifecycleOwner) { reminders ->
            adapter.submitList(reminders)
            // Agora usamos a função que verifica se já foi criado uma vez
            viewModel.checkAndInsertDefaultWaterReminder()
        }

        binding.fabAddReminder.setOnClickListener {
            showAddReminderDialog()
        }
    }

    private fun showAddReminderDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_reminder, null)
        val edtTitle = dialogView.findViewById<EditText>(R.id.edt_reminder_title)
        val spinnerInterval = dialogView.findViewById<Spinner>(R.id.spinner_interval)
        val edtStart = dialogView.findViewById<EditText>(R.id.edt_start_time)
        val edtEnd = dialogView.findViewById<EditText>(R.id.edt_end_time)
        
        // Checkboxes dos dias
        val cbDom = dialogView.findViewById<CheckBox>(R.id.cb_dom)
        val cbSeg = dialogView.findViewById<CheckBox>(R.id.cb_seg)
        val cbTer = dialogView.findViewById<CheckBox>(R.id.cb_ter)
        val cbQua = dialogView.findViewById<CheckBox>(R.id.cb_qua)
        val cbQui = dialogView.findViewById<CheckBox>(R.id.cb_qui)
        val cbSex = dialogView.findViewById<CheckBox>(R.id.cb_sex)
        val cbSab = dialogView.findViewById<CheckBox>(R.id.cb_sab)

        val intervals = (5..60 step 5).toList()
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, intervals)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerInterval.adapter = spinnerAdapter
        spinnerInterval.setSelection(intervals.indexOf(60))

        edtStart.setOnClickListener { showTimePicker { time -> edtStart.setText(time) } }
        edtEnd.setOnClickListener { showTimePicker { time -> edtEnd.setText(time) } }

        AlertDialog.Builder(requireContext())
            .setTitle("Novo Lembrete")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val title = edtTitle.text.toString()
                val interval = spinnerInterval.selectedItem as Int
                val start = edtStart.text.toString().ifEmpty { "08:00" }
                val end = edtEnd.text.toString().ifEmpty { "22:00" }
                
                // Mapeia dias selecionados (Calendar.SUNDAY = 1, etc)
                val selectedDays = mutableListOf<Int>()
                if (cbDom.isChecked) selectedDays.add(1)
                if (cbSeg.isChecked) selectedDays.add(2)
                if (cbTer.isChecked) selectedDays.add(3)
                if (cbQua.isChecked) selectedDays.add(4)
                if (cbQui.isChecked) selectedDays.add(5)
                if (cbSex.isChecked) selectedDays.add(6)
                if (cbSab.isChecked) selectedDays.add(7)

                if (title.isNotBlank() && selectedDays.isNotEmpty()) {
                    viewModel.insert(
                        Reminder(
                            title = title,
                            description = "Lembrete recorrente",
                            intervalMinutes = interval,
                            startTime = start,
                            endTime = end,
                            daysOfWeek = selectedDays.joinToString(",")
                        )
                    )
                } else if (selectedDays.isEmpty()) {
                    Toast.makeText(requireContext(), "Selecione ao menos um dia!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, hour, minute ->
            onTimeSelected(String.format("%02d:%02d", hour, minute))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun showDeleteDialog(reminder: Reminder) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir Lembrete")
            .setMessage("Deseja apagar o lembrete '${reminder.title}'?")
            .setPositiveButton("Sim") { _, _ -> viewModel.delete(reminder) }
            .setNegativeButton("Não", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
