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
import com.przevolut.databinding.FragmentAlertsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Ekran 4/5 — Alerty walutowe.
 * Wyświetla listę alertów, umożliwia tworzenie i usuwanie.
 */
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

        binding.fabAddAlert.setOnClickListener {
            showAddAlertDialog()
        }
    }

    private fun setupRecyclerView() {
        alertsAdapter = AlertsAdapter(
            onDeleteClick = { alert ->
                viewModel.deleteAlert(alert.id)
            }
        )
        binding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = alertsAdapter
        }
    }

    private fun showAddAlertDialog() {
        // TODO: Zastąpić dedykowanym fragmentem AddAlertFragment
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nowy alert")
            .setMessage("Funkcja w budowie — zaimplementuj AddAlertFragment")
            .setPositiveButton("OK", null)
            .show()
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
                        binding.tvEmpty.visibility = if (state.alerts.isEmpty()) View.VISIBLE else View.GONE
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
