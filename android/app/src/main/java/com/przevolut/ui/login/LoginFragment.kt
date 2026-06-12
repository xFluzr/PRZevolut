package com.przevolut.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.przevolut.R
import com.przevolut.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

        binding.btnBiometric.setOnClickListener { showBiometricPrompt() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is LoginUiState.Loading -> {
                        binding.progressIndicator.visibility = View.VISIBLE
                        binding.btnLogin.isEnabled = false
                    }
                    is LoginUiState.Success -> {
                        binding.progressIndicator.visibility = View.GONE
                        viewModel.resetState()
                        findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
                    }
                    is LoginUiState.Error -> {
                        binding.progressIndicator.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        binding.root.announceForAccessibility(state.message)
                    }
                    is LoginUiState.Idle -> {
                        binding.progressIndicator.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                    }
                }
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Podaj prawidłowy adres e-mail"
            binding.etEmail.announceForAccessibility("Podaj prawidłowy adres e-mail")
            return false
        }
        binding.tilEmail.error = null
        if (password.length < 8) {
            binding.tilPassword.error = "Hasło musi mieć co najmniej 8 znaków"
            binding.etPassword.announceForAccessibility("Hasło musi mieć co najmniej 8 znaków")
            return false
        }
        binding.tilPassword.error = null
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
                    Snackbar.make(binding.root, "Błąd: $errString", Snackbar.LENGTH_SHORT).show()
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
