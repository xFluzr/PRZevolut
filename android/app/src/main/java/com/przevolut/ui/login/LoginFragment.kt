package com.przevolut.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.przevolut.R
import com.przevolut.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Ekran 1/5 — Logowanie.
 * Obsługuje: logowanie e-mail+hasło, rejestrację, logowanie biometryczne.
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
        setupBiometricIfAvailable()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (validateInput(email, password)) {
                viewModel.login(email, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (validateInput(email, password)) {
                viewModel.register(email, password)
            }
        }

        binding.btnBiometric.setOnClickListener {
            showBiometricPrompt()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is LoginUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnLogin.isEnabled = false
                    }
                    is LoginUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
                    }
                    is LoginUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                    is LoginUiState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                    }
                }
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Podaj prawidłowy adres e-mail"
            return false
        }
        if (password.length < 8) {
            binding.etPassword.error = "Hasło musi mieć co najmniej 8 znaków"
            return false
        }
        return true
    }

    private fun setupBiometricIfAvailable() {
        val biometricManager = BiometricManager.from(requireContext())
        val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG)
        binding.btnBiometric.visibility = if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Zaloguj się biometrycznie")
            .setSubtitle("Użyj odcisku palca lub twarzy")
            .setNegativeButtonText("Użyj hasła")
            .build()

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.loginWithBiometric()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(requireContext(), "Błąd: $errString", Toast.LENGTH_SHORT).show()
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
