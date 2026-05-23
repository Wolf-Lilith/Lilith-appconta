package com.joaomartins.lilith

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.joaomartins.lilith.databinding.FragmentCallBlockerBinding

class CallBlockerFragment : Fragment() {

    private var _binding: FragmentCallBlockerBinding? = null
    private val binding get() = _binding!!

    private val sharedPrefs by lazy {
        requireContext().getSharedPreferences("call_blocker_prefs", Context.MODE_PRIVATE)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            checkAndRequestRole()
        } else {
            Toast.makeText(requireContext(), "Permissões necessárias para o funcionamento.", Toast.LENGTH_SHORT).show()
        }
    }

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            setEnabled(true)
        } else {
            Toast.makeText(requireContext(), "O Lilith precisa ser o app de chamadas padrão para bloquear.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallBlockerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUi()

        binding.btnToggleBlocker.setOnClickListener {
            val isEnabled = sharedPrefs.getBoolean("enabled", false)
            if (isEnabled) {
                setEnabled(false)
            } else {
                checkPermissions()
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            checkAndRequestRole()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun checkAndRequestRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = requireContext().getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    roleRequestLauncher.launch(intent)
                } else {
                    setEnabled(true)
                }
            }
        } else {
            // Para versões anteriores ao Q, a lógica seria diferente, 
            // mas como minSdk é 30, focamos no CallScreeningService.
            setEnabled(true)
        }
    }

    private fun setEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("enabled", enabled).apply()
        updateUi()
        val status = if (enabled) "ativado" else "desativado"
        Toast.makeText(requireContext(), "Bloqueador $status.", Toast.LENGTH_SHORT).show()
    }

    private fun updateUi() {
        val isEnabled = sharedPrefs.getBoolean("enabled", false)
        if (isEnabled) {
            binding.tvStatusValue.text = "ATIVADO"
            binding.tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.revenue_green))
            binding.btnToggleBlocker.text = "Desativar Bloqueador"
        } else {
            binding.tvStatusValue.text = "DESATIVADO"
            binding.tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red))
            binding.btnToggleBlocker.text = "Ativar Bloqueador"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
