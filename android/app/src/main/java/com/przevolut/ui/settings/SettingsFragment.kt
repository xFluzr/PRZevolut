package com.przevolut.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.przevolut.BuildConfig
import com.przevolut.R
import com.przevolut.databinding.FragmentSettingsBinding
import com.przevolut.ui.common.ThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private val supportedCurrencies = com.przevolut.ui.common.CurrencyUi.SUPPORTED.toList()
    private val refreshOptions = listOf(
        RefreshOption(15, R.string.settings_refresh_15),
        RefreshOption(30, R.string.settings_refresh_30),
        RefreshOption(60, R.string.settings_refresh_60),
        RefreshOption(240, R.string.settings_refresh_240),
    )

    private var suppressCurrencyCallback = false
    private var suppressThemeCallback = false
    private var suppressRefreshCallback = false
    private var suppressBiometricCallback = false

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
        observeEvents()

        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressBiometricCallback) {
                viewModel.setBiometricEnabled(isChecked)
            }
        }

        binding.toggleTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || suppressThemeCallback) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                R.id.btn_theme_light -> "light"
                R.id.btn_theme_dark -> "dark"
                else -> "system"
            }
            viewModel.setThemeMode(mode)
            ThemeHelper.applyTheme(mode)
        }

        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        binding.btnLogout.setOnClickListener { viewModel.logout() }
        binding.tvAppVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshProfile()
    }

    private fun setupCurrencyDropdown() {
        val adapter = NonFilterableArrayAdapter(
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
        val adapter = NonFilterableArrayAdapter(
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

    private fun showChangePasswordDialog() {
        val currentInput = TextInputEditText(requireContext()).apply {
            hint = getString(R.string.dialog_current_password)
        }
        val newInput = TextInputEditText(requireContext()).apply {
            hint = getString(R.string.dialog_new_password)
        }
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val pad = resources.getDimensionPixelSize(R.dimen.spacing_md)
            setPadding(pad, pad, pad, 0)
            addView(TextInputLayout(requireContext()).apply { addView(currentInput) })
            addView(TextInputLayout(requireContext()).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = pad }
                addView(newInput)
            })
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_change_password_title)
            .setView(container)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_confirm_save) { _, _ ->
                val current = currentInput.text?.toString().orEmpty()
                val newPass = newInput.text?.toString().orEmpty()
                if (newPass.length >= 8) {
                    viewModel.changePassword(current, newPass)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Nowe hasło musi mieć co najmniej 8 znaków.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                binding.tvUserEmail.text = settings.userEmail
                    ?: getString(R.string.settings_account_not_logged_in)

                binding.btnChangePassword.isEnabled = settings.isLoggedIn

                suppressBiometricCallback = true
                binding.switchBiometric.isChecked = settings.biometricEnabled
                binding.switchBiometric.isEnabled = settings.isLoggedIn
                suppressBiometricCallback = false

                suppressCurrencyCallback = true
                binding.actvDefaultCurrency.setText(settings.defaultCurrency, false)
                suppressCurrencyCallback = false

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

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is SettingsEvent.Message ->
                        Toast.makeText(requireContext(), event.text, Toast.LENGTH_LONG).show()
                    SettingsEvent.LoggedOut -> {
                        findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
                    }
                }
            }
        }
    }

    private data class RefreshOption(val minutes: Int, val labelRes: Int)

    /**
     * ArrayAdapter subclass that disables filtering entirely.
     *
     * The default ArrayAdapter applies an internal Filter when
     * AutoCompleteTextView.setText() is called, even with the
     * "filter = false" parameter. After Activity recreation (e.g.
     * theme change), the adapter is created fresh, setText restores
     * the saved value, and the internal filter remembers only the
     * matching item — so the dropdown shows just one option.
     *
     * By overriding getFilter() to return a no-op filter, we ensure
     * the dropdown always shows the full list of options.
     */
    private class NonFilterableArrayAdapter<T>(
        context: android.content.Context,
        resource: Int,
        private val allItems: List<T>
    ) : ArrayAdapter<T>(context, resource, allItems) {

        override fun getFilter(): android.widget.Filter {
            return object : android.widget.Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    return FilterResults().apply {
                        values = allItems
                        count = allItems.size
                    }
                }

                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
