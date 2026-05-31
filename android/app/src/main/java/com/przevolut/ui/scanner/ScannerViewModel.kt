package com.przevolut.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.przevolut.data.local.dao.RateDao
import com.przevolut.domain.model.ScanResult
import com.przevolut.utils.CurrencyParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel dla ScannerFragment.
 * Odpowiada za przetwarzanie wyników OCR i przeliczanie kwot.
 *
 * Zastosowano debounce (300ms) na strumieniu tekstu OCR — unikamy
 * wielokrotnego przeliczania tej samej ceny gdy kamera "trzęsie się"
 * między klatkami.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val rateDao: RateDao
) : ViewModel() {

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult

    // Wewnętrzny strumień surowego tekstu z OCR — debounce 300ms
    private val _ocrTextStream = MutableSharedFlow<String>(extraBufferCapacity = 8)

    init {
        viewModelScope.launch {
            _ocrTextStream
                .debounce(300L)
                .collect { rawText ->
                    handleOcrText(rawText)
                }
        }
    }

    /**
     * Przetwarza tekst z OCR ML Kit.
     * Wysyła na strumień z debounce — bezpieczne do wywoływania z każdej klatki.
     */
    fun processOcrResult(rawText: String) {
        _ocrTextStream.tryEmit(rawText)
    }

    private suspend fun handleOcrText(rawText: String) {
        val parsed = CurrencyParser.parse(rawText)
        if (parsed != null) {
            val (amount, currency) = parsed
            val rateEntity = rateDao.getLatestRate(currency)

            _scanResult.value = ScanResult(
                detectedText = rawText,
                detectedAmount = amount,
                detectedCurrency = currency,
                convertedAmountPln = rateEntity?.let { amount * it.rate },
                usedRate = rateEntity?.rate
            )
        }
        // Jeśli nie wykryto — nie czyścimy wyniku, zostawiamy ostatni poprawny wynik na ekranie
    }
}
