package com.przevolut.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.przevolut.data.local.dao.RateDao
import com.przevolut.domain.model.ScanResult
import com.przevolut.utils.CurrencyParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel dla ScannerFragment.
 * Odpowiada za przetwarzanie wyników OCR i przeliczanie kwot.
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val rateDao: RateDao
) : ViewModel() {

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult

    /**
     * Przetwarza tekst z OCR ML Kit.
     * Parsuje kwotę i walutę, pobiera kurs z Room DB, oblicza wartość PLN.
     */
    fun processOcrResult(rawText: String) {
        viewModelScope.launch {
            val parsed = CurrencyParser.parse(rawText)
            if (parsed != null) {
                val (amount, currency) = parsed
                val rateEntity = rateDao.getLatestRate(currency)
                val convertedPln = rateEntity?.let { amount * it.rate }

                _scanResult.value = ScanResult(
                    detectedText = rawText,
                    detectedAmount = amount,
                    detectedCurrency = currency,
                    convertedAmountPln = convertedPln,
                    usedRate = rateEntity?.rate
                )
            } else {
                // Nie wykryto ceny — nie aktualizuj UI
            }
        }
    }
}
