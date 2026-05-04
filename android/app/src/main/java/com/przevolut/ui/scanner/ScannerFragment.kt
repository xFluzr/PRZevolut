package com.przevolut.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Ekran 2/5 — Skaner AR.
 * Używa CameraX do podglądu kamery i ML Kit do OCR w czasie rzeczywistym.
 * Wyświetla przeliczoną cenę jako nakładkę na podglądzie kamery.
 */
@AndroidEntryPoint
class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ScannerViewModel by viewModels()

    private lateinit var cameraExecutor: ExecutorService
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // ── Uprawnienia do aparatu ────────────────────────────────────────────

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(),
                "Uprawnienie do kamery jest wymagane do skanowania cen.", Toast.LENGTH_LONG).show()
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
            // TODO: Pokaż dialog ręcznego wpisania kwoty
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> startCamera()

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(requireContext(),
                    "Aplikacja potrzebuje dostępu do kamery, aby skanować ceny.", Toast.LENGTH_LONG).show()
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
                        processImageForOcr(imageProxy)
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
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                viewModel.processOcrResult(visionText.text)
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
                            "${it.detectedAmount} ${it.detectedCurrency} = %.2f PLN".format(it.convertedAmountPln)
                        binding.tvRateInfo.text =
                            "Kurs: 1 ${it.detectedCurrency} = ${it.usedRate} PLN"
                    } else {
                        binding.tvOverlayResult.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
