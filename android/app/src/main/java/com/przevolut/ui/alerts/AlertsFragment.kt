package com.przevolut.ui.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.przevolut.R
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
        binding.fabAddAlert.setOnClickListener { showAddAlertDialog() }
    }

    private fun setupRecyclerView() {
        alertsAdapter = AlertsAdapter(onDeleteClick = { alert ->
            viewModel.deleteAlert(alert.id)
        })
        binding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertsAdapter
        }
    }

    private fun showAddAlertDialog() {
        val dialogBinding = DialogAddAlertBinding.inflate(layoutInflater)
        val currencyChips = listOf(
            dialogBinding.chipEur to "EUR",
            dialogBinding.chipUsd to "USD",
            dialogBinding.chipGbp to "GBP",
            dialogBinding.chipChf to "CHF",
            dialogBinding.chipCzk to "CZK",
        )
        currencyChips.forEach { (chip, code) ->
            chip.text = CurrencyUi.chipLabel(code)
        }

        var selectedCurrency = "EUR"
        var selectedDirection = "above"

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
        dialogBinding.btnAbove.isChecked = true

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_add_alert_title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_confirm, null)
            .create()

        dialog.setOnShowListener {
            val confirmButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            confirmButton.isEnabled = false

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
                    viewModel.createAlert(selectedCurrency, selectedDirection, threshold)
                    dialog.dismiss()
                }
            }

            dialogBinding.etThreshold.requestFocus()
        }

        dialog.setOnDismissListener {
            binding.fabAddAlert.requestFocus()
        }

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
                        binding.progressBar.visibility = View.GONE
                        alertsAdapter.submitList(state.alerts)
                        val isEmpty = state.alerts.isEmpty()
                        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                        binding.rvAlerts.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    }
                    is AlertsUiState.Error -> {
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
