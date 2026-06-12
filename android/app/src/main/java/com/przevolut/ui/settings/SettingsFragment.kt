package com.przevolut.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.przevolut.BuildConfig
import com.przevolut.R
import com.przevolut.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private val supportedCurrencies = listOf("EUR", "USD", "GBP", "CHF", "CZK")
    private val refreshOptions = listOf(
        RefreshOption(15, R.string.settings_refresh_15),
        RefreshOption(30, R.string.settings_refresh_30),
        RefreshOption(60, R.string.settings_refresh_60),
        RefreshOption(240, R.string.settings_refresh_240),
    )

    private var suppressCurrencyCallback = false
    private var suppressThemeCallback = false
    private var suppressRefreshCallback = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCurrencyDropdown()
        setupRefreshDropdown()
        observeViewModel()

        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBiometricEnabled(isChecked)
        }

        binding.toggleTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || suppressThemeCallback) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.btn_theme_light -> "light"
                R.id.btn_theme_dark -> "dark"
                else -> "system"
            }
            viewModel.setThemeMode(mode)
            applyTheme(mode)
        }

        binding.btnLogout.setOnClickListener { viewModel.logout() }
        binding.tvAppVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }

    private fun setupCurrencyDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            supportedCurrencies
        )
        binding.actvDefaultCurrency.setAdapter(adapter)
        binding.actvDefaultCurrency.setOnItemClickListener { _, _, position, _ ->
            if (!suppressCurrencyCallback) {
                viewModel.setDefaultCurrency(supportedCurrencies[position])
            }
        }
    }

    private fun setupRefreshDropdown() {
        val labels = refreshOptions.map { getString(it.labelRes) }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            labels
        )
        binding.actvRefreshInterval.setAdapter(adapter)
        binding.actvRefreshInterval.setOnItemClickListener { _, _, position, _ ->
            if (!suppressRefreshCallback) {
                viewModel.setRefreshIntervalMinutes(refreshOptions[position].minutes)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                suppressCurrencyCallback = true
                binding.actvDefaultCurrency.setText(settings.defaultCurrency, false)
                suppressCurrencyCallback = false

                binding.switchBiometric.isChecked = settings.biometricEnabled

                suppressThemeCallback = true
                when (settings.themeMode) {
                    "light" -> binding.btnThemeLight.isChecked = true
                    "dark" -> binding.btnThemeDark.isChecked = true
                    else -> binding.btnThemeSystem.isChecked = true
                }
                suppressThemeCallback = false

                suppressRefreshCallback = true
                val refreshLabel = refreshOptions
                    .firstOrNull { it.minutes == settings.refreshIntervalMinutes }
                    ?.labelRes
                    ?.let { getString(it) }
                    ?: getString(R.string.settings_refresh_60)
                binding.actvRefreshInterval.setText(refreshLabel, false)
                suppressRefreshCallback = false
            }
        }
    }

    private fun applyTheme(mode: String) {
        val nightMode = when (mode) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private data class RefreshOption(val minutes: Int, val labelRes: Int)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
