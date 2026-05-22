package com.joaomartins.lilith

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
        loadUserBackground()
    }

    /**
     * Busca a URI da imagem salva no SharedPreferences e aplica no fundo da Home.
     */
    private fun loadUserBackground() {
        val prefs = requireContext().getSharedPreferences("lilith_prefs", Context.MODE_PRIVATE)
        val bgUriString = prefs.getString("bg_home_uri", null)

        if (bgUriString != null) {
            try {
                val uri = Uri.parse(bgUriString)
                // Aplica a imagem da galeria no ImageView que criamos no XML
                binding.imgBackground.setImageURI(uri)

                // Garantimos que a opacidade (alpha) seja aplicada mesmo na imagem nova
                binding.imgBackground.alpha = 0.4f
            } catch (e: Exception) {
                // Se der erro (ex: imagem deletada do celular), volta para o padrão
                binding.imgBackground.setImageResource(R.drawable.logo_lilith)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}