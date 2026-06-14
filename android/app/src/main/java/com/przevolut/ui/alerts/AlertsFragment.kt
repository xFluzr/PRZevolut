package com.przevolut.ui.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.przevolut.R
import com.przevolut.data.remote.model.AlertResponse
import com.przevolut.databinding.DialogAddAlertBinding
import com.przevolut.databinding.FragmentAlertsBinding
import com.przevolut.ui.common.CurrencyUi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AlertsViewModel by viewModels()
    private lateinit var alertsAdapter: AlertsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        setupInsets()
        binding.btnAddAlert.setOnClickListener { showAlertDialog() }
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadAlerts() }
    }

    /**
     * Dynamically adjust RecyclerView and empty-state bottom padding to account
     * for the system navigation bar height (gesture nav varies per device).
     */
    private fun setupInsets() {
        val baseMargin  = resources.getDimensionPixelSize(R.dimen.spacing_lg)  // 24dp
        val navBarHeight = resources.getDimensionPixelSize(R.dimen.bottom_nav_height) // 64dp

        ViewCompat.setOnApplyWindowInsetsListener(binding.rvAlerts) { rv, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            rv.setPadding(
                rv.paddingLeft, rv.paddingTop, rv.paddingRight,
                navBarHeight + systemBars.bottom + baseMargin
            )
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutEmpty) { layout, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            layout.setPadding(
                layout.paddingLeft, layout.paddingTop, layout.paddingRight,
                navBarHeight + systemBars.bottom + baseMargin
            )
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAlerts()
    }

    private fun setupRecyclerView() {
        alertsAdapter = AlertsAdapter(
            onEditClick = { alert -> showAlertDialog(alert) },
            onDeleteClick = { alert -> viewModel.deleteAlert(alert.id) },
            onToggleActiveClick = { alert -> viewModel.toggleAlertStatus(alert.id, !alert.isActive) }
        )
        binding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertsAdapter
        }
    }

    private fun showAlertDialog(existing: AlertResponse? = null) {
        val dialogBinding = DialogAddAlertBinding.inflate(layoutInflater)
        val watched = viewModel.getWatchedCurrencies()
        val defaultCurrency = viewModel.getDefaultCurrency()
        val sortedWatched = watched.toList().sortedWith(compareBy({ it != defaultCurrency }, { it }))
        
        val currencyChips = mutableListOf<Pair<com.google.android.material.chip.Chip, String>>()
        dialogBinding.chipGroupCurrency.removeAllViews()
        
        sortedWatched.forEach { code ->
            val chip = com.google.android.material.chip.Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
                text = CurrencyUi.chipLabel(code)
                isCheckable = true
                isCheckedIconVisible = false
                id = View.generateViewId()
            }
            currencyChips.add(chip to code)
            dialogBinding.chipGroupCurrency.addView(chip)
        }

        var selectedCurrency = existing?.currency ?: defaultCurrency
        // Upewnij się, że wybrana waluta jest faktycznie na liście chipów (np. jeśli usunęliśmy z obserwowanych)
        if (selectedCurrency !in watched) {
            selectedCurrency = defaultCurrency
        }
        var selectedDirection = existing?.direction ?: "above"

        if (existing != null) {
            dialogBinding.chipGroupCurrency.isEnabled = false
            currencyChips.forEach { (chip, code) ->
                chip.isChecked = code == existing.currency
            }
            dialogBinding.etThreshold.setText("%.4f".format(existing.threshold))
            if (existing.direction == "below") {
                dialogBinding.btnBelow.isChecked = true
            } else {
                dialogBinding.btnAbove.isChecked = true
            }
        } else {
            currencyChips.find { it.second == defaultCurrency }?.first?.isChecked = true
            dialogBinding.btnAbove.isChecked = true
        }

        dialogBinding.chipGroupCurrency.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            selectedCurrency = currencyChips.first { it.first.id == checkedId }.second
        }

        dialogBinding.toggleDirection.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedDirection = when (checkedId) {
                    R.id.btn_below -> "below"
                    else -> "above"
                }
            }
        }

        val isEdit = existing != null
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isEdit) R.string.dialog_edit_alert_title else R.string.dialog_add_alert_title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(
                if (isEdit) R.string.dialog_confirm_save else R.string.dialog_confirm,
                null
            )
            .create()

        dialog.setOnShowListener {
            val confirmButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            confirmButton.isEnabled = existing != null

            dialogBinding.etThreshold.addTextChangedListener(
                object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        val value = s?.toString()?.toDoubleOrNull()
                        confirmButton.isEnabled = value != null && value > 0
                    }
                }
            )

            confirmButton.setOnClickListener {
                val threshold = dialogBinding.etThreshold.text?.toString()?.toDoubleOrNull()
                if (threshold != null && threshold > 0) {
                    if (isEdit) {
                        viewModel.updateAlert(existing!!.id, selectedDirection, threshold)
                    } else {
                        viewModel.createAlert(selectedCurrency, selectedDirection, threshold)
                    }
                    dialog.dismiss()
                }
            }

            // Explicitly show the soft keyboard so it appears reliably on all
            // Android versions (SOFT_INPUT_ADJUST_RESIZE is deprecated since API 30).
            dialogBinding.etThreshold.requestFocus()
            dialogBinding.etThreshold.postDelayed({
                val imm = requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(dialogBinding.etThreshold, InputMethodManager.SHOW_IMPLICIT)
            }, 100)
        }

        dialog.setOnDismissListener { }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AlertsUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is AlertsUiState.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        alertsAdapter.submitList(state.alerts)
                        val isEmpty = state.alerts.isEmpty()
                        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                        binding.rvAlerts.visibility = if (isEmpty) View.GONE else View.VISIBLE
                        if (isEmpty) {
                            binding.tvEmpty.text = getString(R.string.no_alerts_body) +
                                "\n\n" + getString(R.string.alerts_tap_to_edit)
                        }
                    }
                    is AlertsUiState.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
