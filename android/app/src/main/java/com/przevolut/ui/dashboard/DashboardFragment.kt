package com.przevolut.ui.dashboard

import android.graphics.Color
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
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
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
    private val currencyChips = mutableListOf<Pair<Chip, String>>()
    private val dateFormat = SimpleDateFormat("d.MM", Locale("pl", "PL"))
    private var chartTimestamps: List<Long> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader()
        setupCurrencyChips()
        setupRecyclerView()
        setupChartInteraction()
        observeViewModel()
        binding.swipeRefresh.setOnRefreshListener { viewModel.refreshRates() }
    }

    private fun setupHeader() {
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("pl", "PL"))
        binding.tvDate.text = dateFormat.format(Date())
    }

    private fun setupCurrencyChips() {
        binding.chipGroupCurrencies.removeAllViews()
        currencyChips.clear()
        CurrencyUi.SUPPORTED.forEach { code ->
            val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
                text = CurrencyUi.chipLabel(code)
                isCheckable = true
                isCheckedIconVisible = true
                setOnClickListener { viewModel.toggleCurrency(code) }
            }
            currencyChips.add(chip to code)
            binding.chipGroupCurrencies.addView(chip)
        }
    }

    private fun setupRecyclerView() {
        ratesAdapter = RatesAdapter()
        binding.rvRates.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ratesAdapter
        }
    }

    private fun setupChartInteraction() {
        binding.lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e == null || chartTimestamps.isEmpty()) return
                val dataSet = binding.lineChart.data?.getDataSetByIndex(h?.dataSetIndex ?: 0)
                val label = dataSet?.label ?: ""
                val index = e.x.toInt().coerceIn(0, chartTimestamps.lastIndex)
                val date = dateFormat.format(Date(chartTimestamps[index]))
                Snackbar.make(
                    binding.cardChart,
                    "1 $label = ${"%.4f".format(e.y)} PLN ($date)",
                    Snackbar.LENGTH_SHORT
                ).show()
            }

            override fun onNothingSelected() = Unit
        })
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

                        updateCurrencyChips(state.watchedCurrencies)
                        ratesAdapter.submitList(state.rates)
                        binding.tvCurrenciesCount.text = getString(
                            R.string.dashboard_currencies_count,
                            state.rates.size
                        )
                        setupChart(state.chartSeries)

                        if (state.isOffline) {
                            binding.tvOfflineBanner.visibility = View.VISIBLE
                            binding.tvOfflineBanner.text = getString(
                                R.string.offline_banner,
                                state.lastUpdated
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

    private fun updateCurrencyChips(watched: Set<String>) {
        currencyChips.forEach { (chip, code) ->
            chip.isChecked = code in watched
        }
    }

    private fun setupChart(series: Map<String, List<ChartPoint>>) {
        val nonEmpty = series.filter { it.value.isNotEmpty() }
        if (nonEmpty.isEmpty()) {
            binding.lineChart.visibility = View.GONE
            binding.tvChartEmpty.visibility = View.VISIBLE
            return
        }

        binding.lineChart.visibility = View.VISIBLE
        binding.tvChartEmpty.visibility = View.GONE

        chartTimestamps = nonEmpty.values.first().map { it.timestamp }
        val dateLabels = chartTimestamps.map { dateFormat.format(Date(it)) }.toTypedArray()

        val axisLabelColor = ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
        val axisStrokeColor = ContextCompat.getColor(requireContext(), R.color.outline)
        val dataSets = nonEmpty.map { (currency, points) ->
            val entries = points.mapIndexed { index, point ->
                Entry(index.toFloat(), point.ratePln)
            }
            LineDataSet(entries, currency).apply {
                color = ContextCompat.getColor(requireContext(), CurrencyUi.colorRes(currency))
                setCircleColor(ContextCompat.getColor(requireContext(), CurrencyUi.colorRes(currency)))
                lineWidth = 2.5f
                circleRadius = 3.5f
                setDrawCircleHole(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                highLightColor = resolveAttrColor(requireContext(), com.google.android.material.R.attr.colorPrimary)
            }
        }

        binding.lineChart.apply {
            clear()
            data = LineData(dataSets)
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setNoDataText(getString(R.string.dashboard_chart_empty))
            setExtraOffsets(48f, 12f, 16f, 32f)
            setMinOffset(12f)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(true)
                setDrawLabels(true)
                textColor = axisLabelColor
                axisLineColor = axisStrokeColor
                textSize = 11f
                yOffset = 8f
                granularity = 1f
                isGranularityEnabled = true
                setAvoidFirstLastClipping(true)
                axisMinimum = 0f
                axisMaximum = (chartTimestamps.size - 1).coerceAtLeast(0).toFloat()
                setLabelCount(minOf(6, chartTimestamps.size).coerceAtLeast(2), true)
                valueFormatter = IndexAxisValueFormatter(dateLabels)
            }
            axisLeft.apply {
                setDrawGridLines(true)
                setDrawAxisLine(true)
                setDrawLabels(true)
                setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                gridColor = Color.argb(50, 168, 176, 188)
                textColor = axisLabelColor
                axisLineColor = axisStrokeColor
                textSize = 11f
                xOffset = 10f
                spaceTop = 10f
                spaceBottom = 10f
                setLabelCount(5, true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) = "%.2f".format(value)
                }
            }
            axisRight.isEnabled = false
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                textColor = axisLabelColor
            }

            contentDescription = nonEmpty.entries.joinToString(", ") { (code, pts) ->
                val latest = pts.lastOrNull()?.ratePln
                "$code: ${latest?.let { "%.4f".format(it) } ?: "—"} PLN"
            }

            notifyDataSetChanged()
            if (shouldAnimate()) animateX(400) else invalidate()
            post { invalidate() }
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
