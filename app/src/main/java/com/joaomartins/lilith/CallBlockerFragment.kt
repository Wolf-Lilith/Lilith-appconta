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
        // Usando o valor 0 (MODE_PRIVATE) para evitar erro de referência do compilador
        requireContext().getSharedPreferences("call_blocker_prefs", 0)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            checkAndRequestRole()
        } else {
            Toast.makeText(requireContext(), "Permissão necessária para o bloqueador funcionar.", Toast.LENGTH_LONG).show()
            updateUi()
        }
    }

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            setEnabled(true)
        } else {
            Toast.makeText(requireContext(), "O Lilith precisa ser o app de chamadas padrão.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCallBlockerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUi()

        binding.btnToggleBlocker.setOnClickListener {
            val isEnabled = sharedPrefs.getBoolean("enabled", false)
            val permsOk = hasAllPermissions()
            val isRoleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = requireContext().getSystemService(RoleManager::class.java)
                roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
            } else {
                true
            }

            if (isEnabled && permsOk && isRoleHeld) {
                // Se está tudo ok e ativo, o usuário quer desativar
                setEnabled(false)
            } else {
                // Se está desativado OU está ativo mas faltam permissões, tentamos ativar/corrigir
                checkPermissions()
            }
        }
    }

    private fun hasAllPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) permissions.add(Manifest.permission.ANSWER_PHONE_CALLS)

        return permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) permissions.add(Manifest.permission.ANSWER_PHONE_CALLS)

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            checkAndRequestRole()
        } else {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun checkAndRequestRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = requireContext().getSystemService(RoleManager::class.java)
            if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleRequestLauncher.launch(intent)
            } else {
                setEnabled(true)
            }
        } else {
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
        val hasPerms = hasAllPermissions()
        val isRoleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = requireContext().getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            true
        }

        when {
            isEnabled && (!hasPerms || !isRoleHeld) -> {
                binding.tvStatusValue.text = "ERRO DE PERMISSÃO"
                binding.tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark))
                binding.btnToggleBlocker.text = "Corrigir Permissões"
            }
            isEnabled -> {
                binding.tvStatusValue.text = "ATIVADO"
                binding.tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.revenue_green))
                binding.btnToggleBlocker.text = "Desativar Bloqueador"
            }
            else -> {
                binding.tvStatusValue.text = "DESATIVADO"
                binding.tvStatusValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red))
                binding.btnToggleBlocker.text = "Ativar Bloqueador"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}