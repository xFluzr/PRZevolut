package com.przevolut.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.przevolut.databinding.FragmentScannerBinding
import com.przevolut.utils.PriceDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Ekran 2/5 — Skaner AR.
 * Używa CameraX do podglądu kamery i ML Kit do OCR w czasie rzeczywistym.
 * Wyświetla przeliczoną cenę jako nakładkę AR (ArOverlayView) na podglądzie kamery.
 */
@AndroidEntryPoint
class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ScannerViewModel by viewModels()

    private lateinit var cameraExecutor: ExecutorService
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Throttle: przetwarzamy OCR max co 500ms żeby nie zalewać ViewModelu
    private var lastOcrTimestamp = 0L
    private val OCR_THROTTLE_MS = 500L

    // ── Uprawnienia do aparatu ────────────────────────────────────────────

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                "Uprawnienie do kamery jest wymagane do skanowania cen.",
                Toast.LENGTH_LONG
            ).show()
            binding.tvPermissionMessage.visibility = View.VISIBLE
            binding.previewView.visibility = View.GONE
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

        checkCameraPermission()
        observeViewModel()

        binding.btnManualInput.setOnClickListener {
            showManualInputDialog()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> startCamera()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    requireContext(),
                    "Aplikacja potrzebuje dostępu do kamery, aby skanować ceny.",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        binding.tvPermissionMessage.visibility = View.GONE
        binding.previewView.visibility = View.VISIBLE

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val now = System.currentTimeMillis()
                        if (now - lastOcrTimestamp >= OCR_THROTTLE_MS) {
                            lastOcrTimestamp = now
                            processImageForOcr(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
                Log.d("ScannerFragment", "Kamera uruchomiona pomyślnie.")
            } catch (e: Exception) {
                Log.e("ScannerFragment", "Błąd uruchamiania kamery: ${e.message}", e)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
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
                val raw = visionText.text
                if (raw.isNotBlank()) {
                    Log.d("ScannerOCR", "Wykryty tekst: $raw")
                    viewModel.processOcrResult(raw)

                    // AR Overlay: wykryj ceny z pozycjami
                    val detectedPrices = PriceDetector.detect(
                        visionText, imageWidth, imageHeight
                    )
                    val rates = viewModel.ratesMap.value
                    binding.arOverlay.updatePrices(detectedPrices, rates)

                    // a11y: Announce detected price for TalkBack users
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
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scanResult.collect { result ->
                result?.let {
                    if (it.convertedAmountPln != null && it.detectedCurrency != null) {
                        binding.tvOverlayResult.visibility = View.VISIBLE
                        binding.tvOverlayResult.text =
                            "%.2f %s = %.2f PLN".format(
                                it.detectedAmount,
                                it.detectedCurrency,
                                it.convertedAmountPln
                            )
                        binding.tvRateInfo.text =
                            "Kurs: 1 ${it.detectedCurrency} = %.4f PLN".format(it.usedRate ?: 0.0)
                    } else {
                        binding.tvOverlayResult.visibility = View.GONE
                        binding.tvRateInfo.text = ""
                    }
                }
            }
        }
    }

    /**
     * Dialog ręcznego wpisania kwoty i waluty.
     * Przydatny gdy OCR nie może odczytać ceny (słabe oświetlenie, niestandardowa czcionka).
     */
    private fun showManualInputDialog() {
        val input = EditText(requireContext()).apply {
            hint = "np. 49.99 EUR lub €12,50"
            textSize = 16f
            setPadding(48, 32, 48, 16)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
