package com.przevolut.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.przevolut.R
import com.przevolut.databinding.FragmentScannerBinding
import com.przevolut.ui.common.CurrencyUi
import com.przevolut.utils.PriceDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ScannerViewModel by viewModels()

    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var lastOcrTimestamp = 0L
    private val ocrThrottleMs = 500L
    private var selectedCurrency = "EUR"

    private val currencyChips = mutableListOf<Pair<Chip, String>>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            showPermissionDenied()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setupCurrencyChips()
        setupFabCollapse()
        checkCameraPermission()
        observeViewModel()

        binding.fabManualInput.setOnClickListener { showManualInputDialog() }
        binding.btnRetryPermission.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onStop() {
        stopCamera()
        super.onStop()
    }

    private fun setupCurrencyChips() {
        currencyChips.clear()
        currencyChips.addAll(
            listOf(
                binding.chipEur to "EUR",
                binding.chipUsd to "USD",
                binding.chipGbp to "GBP",
                binding.chipChf to "CHF",
                binding.chipCzk to "CZK",
            )
        )
        currencyChips.forEach { (chip, code) ->
            chip.text = CurrencyUi.chipLabel(code)
        }

        binding.chipGroupCurrency.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            selectedCurrency = currencyChips.first { it.first.id == checkedId }.second
            updateTopBar(viewModel.ratesMap.value[selectedCurrency])
        }
    }

    private fun setupFabCollapse() {
        binding.fabManualInput.postDelayed({
            _binding?.fabManualInput?.shrink()
        }, 3000)
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED -> startCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> showPermissionDenied()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showPermissionDenied() {
        val b = _binding ?: return
        b.layoutPermission.visibility = View.VISIBLE
        b.previewView.visibility = View.GONE
    }

    private fun startCamera() {
        if (!isAdded || _binding == null) return

        binding.layoutPermission.visibility = View.GONE
        binding.previewView.visibility = View.VISIBLE

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            if (!isAdded || _binding == null) return@addListener

            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                        val now = System.currentTimeMillis()
                        if (now - lastOcrTimestamp >= ocrThrottleMs) {
                            lastOcrTimestamp = now
                            processImageForOcr(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("ScannerFragment", "Błąd uruchamiania kamery: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.w("ScannerFragment", "Błąd zatrzymywania kamery: ${e.message}")
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageForOcr(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val imageWidth = imageProxy.width
        val imageHeight = imageProxy.height

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                if (!isBindingActive()) return@addOnSuccessListener

                val reconstructedText = PriceDetector.reconstructVisionText(visionText)
                if (reconstructedText.isNotBlank()) {
                    viewModel.processOcrResult(reconstructedText)

                    val detectedPrices = PriceDetector.detect(visionText, imageWidth, imageHeight)
                    val rates = viewModel.ratesMap.value
                    binding.arOverlay.updatePrices(detectedPrices, rates)

                    if (detectedPrices.isNotEmpty() && rates.isNotEmpty()) {
                        val first = detectedPrices.first()
                        val rate = rates[first.currency]
                        if (rate != null) {
                            val plnAmount = first.amount * rate
                            binding.arOverlay.announceForAccessibility(
                                "Wykryto %.2f %s, to jest %.2f złotych".format(
                                    first.amount, first.currency, plnAmount
                                )
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ScannerFragment", "OCR error: ${e.message}")
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun isBindingActive(): Boolean {
        return _binding != null && isAdded && viewLifecycleOwner.lifecycle.currentState.isAtLeast(
            Lifecycle.State.STARTED
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.ratesMap.collect { rates ->
                        if (isBindingActive()) {
                            updateTopBar(rates[selectedCurrency])
                        }
                    }
                }
                launch {
                    viewModel.scanResult.collect { result ->
                        if (!isBindingActive()) return@collect
                        result?.let {
                            if (it.convertedAmountPln != null && it.detectedCurrency != null) {
                                showResultCard(it)
                            } else {
                                hideResultCard()
                            }
                        } ?: hideResultCard()
                    }
                }
            }
        }
    }

    private fun updateTopBar(rate: Double?) {
        if (!isBindingActive()) return
        binding.tvSelectedCurrency.text = getString(
            R.string.scanner_selected_currency,
            selectedCurrency
        )
        binding.tvRateInfo.text = if (rate != null) {
            getString(R.string.scanner_rate_info, selectedCurrency, "%.4f".format(rate))
        } else {
            ""
        }
        binding.topBar.contentDescription = getString(
            R.string.scanner_rate_info,
            selectedCurrency,
            rate?.let { "%.4f".format(it) } ?: "—"
        )
    }

    private fun showResultCard(result: com.przevolut.domain.model.ScanResult) {
        if (!isBindingActive()) return
        binding.cardResult.visibility = View.VISIBLE
        if (shouldAnimate()) {
            binding.cardResult.translationY = binding.cardResult.height.toFloat()
            binding.cardResult.animate()
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        binding.tvDetectedAmount.text = getString(
            R.string.scanner_result_format,
            "%.2f".format(result.detectedAmount ?: 0.0),
            result.detectedCurrency ?: ""
        )
        binding.tvConvertedPln.text = getString(
            R.string.scanner_result_pln,
            "%.2f".format(result.convertedAmountPln ?: 0.0)
        )
        binding.tvUsedRate.text = getString(
            R.string.scanner_used_rate,
            "%.4f".format(result.usedRate ?: 0.0)
        )
    }

    private fun hideResultCard() {
        _binding?.cardResult?.visibility = View.GONE
    }

    private fun showManualInputDialog() {
        val input = EditText(requireContext()).apply {
            hint = "np. 49.99 EUR lub €12,50"
            setPadding(
                resources.getDimensionPixelSize(R.dimen.spacing_xl),
                resources.getDimensionPixelSize(R.dimen.spacing_lg),
                resources.getDimensionPixelSize(R.dimen.spacing_xl),
                resources.getDimensionPixelSize(R.dimen.spacing_md)
            )
            contentDescription = "Pole do ręcznego wpisania ceny"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Wpisz kwotę ręcznie")
            .setView(input)
            .setPositiveButton("Przelicz") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    viewModel.processOcrResult(text)
                } else {
                    Toast.makeText(requireContext(), "Wpisz kwotę", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
            .also { dialog ->
                dialog.setOnShowListener { input.requestFocus() }
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
        stopCamera()
        cameraProvider = null
        cameraExecutor?.shutdown()
        cameraExecutor = null
        _binding = null
        super.onDestroyView()
    }
}
