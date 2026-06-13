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
import com.przevolut.ui.common.ThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    private var isRegisterMode = false
    private var hasNavigatedAfterLogin = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupThemeToggle()
        setupAuthModeToggle()
        setupClickListeners()
        observeViewModel()
        setupBiometricIfAvailable()
    }

    private fun setupThemeToggle() {
        updateThemeToggleIcon()
        binding.btnThemeToggle.setOnClickListener {
            ThemeHelper.toggleLightDark(requireContext())
            requireActivity().recreate()
        }
    }

    private fun updateThemeToggleIcon() {
        val isDark = ThemeHelper.isDarkMode(requireContext())
        binding.btnThemeToggle.setIconResource(if (isDark) R.drawable.ic_sun else R.drawable.ic_moon)
        binding.btnThemeToggle.contentDescription = getString(
            if (isDark) R.string.cd_theme_light else R.string.cd_theme_dark
        )
    }

    private fun setupAuthModeToggle() {
        binding.btnModeLogin.isChecked = true
        binding.toggleAuthMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            isRegisterMode = checkedId == R.id.btn_mode_register
            binding.btnSubmit.text = getString(
                if (isRegisterMode) R.string.btn_register else R.string.btn_login
            )
            binding.btnSubmit.contentDescription = binding.btnSubmit.text
        }
    }

    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            if (validateInput(email, password)) {
                if (isRegisterMode) {
                    viewModel.register(email, password)
                } else {
                    viewModel.login(email, password)
                }
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
                        binding.btnSubmit.isEnabled = false
                    }
                    is LoginUiState.Success -> {
                        binding.progressIndicator.visibility = View.GONE
                        if (hasNavigatedAfterLogin) return@collect
                        hasNavigatedAfterLogin = true
                        viewModel.resetState()
                        val navController = findNavController()
                        if (navController.currentDestination?.id == R.id.loginFragment) {
                            navController.navigate(R.id.action_loginFragment_to_dashboardFragment)
                        }
                    }
                    is LoginUiState.Error -> {
                        binding.progressIndicator.visibility = View.GONE
                        binding.btnSubmit.isEnabled = true
                        Snackbar.make(binding.cardLogin, state.message, Snackbar.LENGTH_LONG).show()
                        binding.cardLogin.announceForAccessibility(state.message)
                    }
                    is LoginUiState.Idle -> {
                        binding.progressIndicator.visibility = View.GONE
                        binding.btnSubmit.isEnabled = true
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
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.loginWithBiometric()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Snackbar.make(binding.cardLogin, "Błąd: $errString", Snackbar.LENGTH_SHORT).show()
                }
            })
        biometricPrompt.authenticate(promptInfo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
