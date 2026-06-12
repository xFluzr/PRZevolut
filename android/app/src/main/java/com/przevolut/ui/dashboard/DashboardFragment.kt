package com.przevolut.ui.dashboard

import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.przevolut.R
import com.przevolut.data.local.entity.RateEntity
import com.przevolut.databinding.FragmentDashboardBinding
import com.przevolut.ui.common.CurrencyUi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        setupHeader()
        setupRecyclerView()
        observeViewModel()
        binding.swipeRefresh.setOnRefreshListener { viewModel.refreshRates() }
    }

    private fun setupHeader() {
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("pl", "PL"))
        binding.tvDate.text = dateFormat.format(Date())
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
                        binding.cardChart.visibility = View.GONE
                        binding.tvError.visibility = View.GONE
                    }
                    is DashboardUiState.Success -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        binding.rvRates.visibility = View.VISIBLE
                        binding.cardChart.visibility = View.VISIBLE
                        binding.tvError.visibility = View.GONE

                        ratesAdapter.submitList(state.rates)
                        binding.tvCurrenciesCount.text = getString(
                            R.string.dashboard_currencies_count,
                            state.rates.size
                        )
                        setupChart(state.rates)

                        if (state.isOffline) {
                            binding.tvOfflineBanner.visibility = View.VISIBLE
                            binding.tvOfflineBanner.text = getString(
                                R.string.offline_banner,
                                state.lastUpdated
                            )
                            binding.tvOfflineBanner.announceForAccessibility(
                                getString(R.string.offline_banner, state.lastUpdated)
                            )
                        } else {
                            binding.tvOfflineBanner.visibility = View.GONE
                        }
                    }
                    is DashboardUiState.Error -> {
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE
                        binding.rvRates.visibility = View.GONE
                        binding.cardChart.visibility = View.GONE
                        binding.tvError.visibility = View.VISIBLE
                        binding.tvError.text = state.message
                    }
                }
            }
        }
    }

    private fun setupChart(rates: List<RateEntity>) {
        if (rates.isEmpty()) return

        val entries = rates.mapIndexed { index, rate ->
            BarEntry(index.toFloat(), rate.mid.toFloat())
        }
        val colors = rates.map { rate ->
            ContextCompat.getColor(requireContext(), CurrencyUi.colorRes(rate.currency))
        }

        val dataSet = BarDataSet(entries, "").apply {
            this.colors = colors
            valueTextColor = resolveAttrColor(requireContext(), android.R.attr.textColorPrimary)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = "%.2f".format(value)
            }
        }

        binding.barChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(rates.map { it.currency })
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = resolveAttrColor(requireContext(), android.R.attr.textColorSecondary)
            }
            axisLeft.apply {
                setDrawGridLines(false)
                textColor = resolveAttrColor(requireContext(), android.R.attr.textColorSecondary)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) = "%.2f PLN".format(value)
                }
            }
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = true
            legend.textColor = resolveAttrColor(requireContext(), android.R.attr.textColorSecondary)
            setFitBars(true)

            contentDescription = rates.joinToString(", ") {
                "${it.currency}: ${"%.4f".format(it.mid)} PLN"
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

            if (shouldAnimate()) animateY(500)
            invalidate()
        }
    }

    private fun shouldAnimate(): Boolean {
        val scale = Settings.Global.getFloat(
            requireContext().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        return scale > 0f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
