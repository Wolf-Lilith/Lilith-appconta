package com.joaomartins.lilith

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.joaomartins.lilith.databinding.FragmentAddBinding
import java.text.SimpleDateFormat
import java.util.*

class AddAccountFragment : Fragment() {

    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!

    // Variável para armazenar a data selecionada no calendário
    private val calendar = Calendar.getInstance()

    // Inicializa o ViewModel através da Factory
    private val viewModel: AccountViewModel by viewModels {
        AccountViewModelFactory((requireActivity().application as LilithApplication).repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configura o clique no campo de data para abrir o Calendário
        binding.editDate.setOnClickListener {
            showDatePicker()
        }

        // Também garante que abra se o campo ganhar foco (navegação via teclado)
        binding.editDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showDatePicker()
        }

        binding.btnSave.setOnClickListener {
            val desc = binding.editDescription.text.toString()
            val valueStr = binding.editValue.text.toString()
            val dateStr = binding.editDate.text.toString()
            val parcelsStr = binding.editParcels.text.toString()
            val isRevenue = binding.radioRevenue.isChecked

            if (desc.isNotEmpty() && valueStr.isNotEmpty() && dateStr.isNotEmpty()) {
                val value = valueStr.toDoubleOrNull() ?: 0.0
                val totalParcels = parcelsStr.toIntOrNull() ?: 1

                // Chamamos a função no ViewModel usando o timestamp do calendário
                viewModel.insertWithParcels(desc, value, isRevenue, calendar.timeInMillis, totalParcels)

                Toast.makeText(requireContext(), "Lançamento salvo!", Toast.LENGTH_SHORT).show()

                // AJUSTE: Limpa os campos para novo cadastro em vez de fechar a tela
                clearFields()
            } else {
                Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.editDate.setText(sdf.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun clearFields() {
        binding.editDescription.text?.clear()
        binding.editValue.text?.clear()
        // Mantemos a data e parcelas preenchidas para facilitar cadastros repetidos
        binding.editDescription.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}