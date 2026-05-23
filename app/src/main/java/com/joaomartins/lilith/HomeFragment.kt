package com.joaomartins.lilith

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.joaomartins.lilith.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupNavigation()
    }

    private fun setupNavigation() {
        // Navegação para o Gerenciador de Finanças
        binding.cardFinance.setOnClickListener {
            findNavController().navigate(R.id.nav_list)
        }

        // Navegação para o Gerenciador de Tarefas
        binding.cardTasks.setOnClickListener {
            findNavController().navigate(R.id.nav_tasks)
        }

        // Navegação para o Gerenciador de Lembretes
        binding.cardReminders.setOnClickListener {
            findNavController().navigate(R.id.nav_reminders)
        }

        // Navegação para o Bloqueador de Chamadas
        binding.cardCallBlocker.setOnClickListener {
            findNavController().navigate(R.id.nav_call_blocker)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
