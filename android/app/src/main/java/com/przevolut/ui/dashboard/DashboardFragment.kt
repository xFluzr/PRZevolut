package com.przevolut.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.przevolut.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Ekran 3/5 — Dashboard z aktualnymi kursami walut.
 * Obsługuje stan loading, dane offline i odświeżanie pull-to-refresh.
 */
@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var ratesAdapter: RatesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshRates()
        }
    }

    private fun setupRecyclerView() {
        ratesAdapter = RatesAdapter()
        binding.rvRates.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ratesAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is DashboardUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.rvRates.visibility = View.GONE
                        binding.tvError.visibility = View.GONE
                    }
                    is DashboardUiState.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        binding.rvRates.visibility = View.VISIBLE
                        binding.tvError.visibility = View.GONE

                        ratesAdapter.submitList(state.rates)

                        // Baner offline
                        if (state.isOffline) {
                            binding.tvOfflineBanner.visibility = View.VISIBLE
                            binding.tvOfflineBanner.text =
                                "📴 Tryb offline — kursy z ${state.lastUpdated}"
                        } else {
                            binding.tvOfflineBanner.visibility = View.GONE
                        }
                    }
                    is DashboardUiState.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        binding.rvRates.visibility = View.GONE
                        binding.tvError.visibility = View.VISIBLE
                        binding.tvError.text = state.message
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
