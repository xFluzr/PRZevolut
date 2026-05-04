package com.przevolut.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.przevolut.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Ekran 5/5 — Ustawienia.
 * Umożliwia: wybór domyślnej waluty, toggle biometrii, wylogowanie, o aplikacji.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private val supportedCurrencies = listOf("EUR", "USD", "GBP", "CHF", "CZK")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCurrencySpinner()
        observeViewModel()

        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBiometricEnabled(isChecked)
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }

        binding.tvAppVersion.text = "PRZevolut v1.0.0"
    }

    private fun setupCurrencySpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            supportedCurrencies
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerDefaultCurrency.adapter = adapter

        binding.spinnerDefaultCurrency.onItemSelectedListener = object :
            android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setDefaultCurrency(supportedCurrencies[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                val currencyIndex = supportedCurrencies.indexOf(settings.defaultCurrency)
                if (currencyIndex >= 0) {
                    binding.spinnerDefaultCurrency.setSelection(currencyIndex)
                }
                binding.switchBiometric.isChecked = settings.biometricEnabled
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
