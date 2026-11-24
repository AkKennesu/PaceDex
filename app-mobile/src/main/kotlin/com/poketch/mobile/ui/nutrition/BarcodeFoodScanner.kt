package com.poketch.mobile.ui.nutrition

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

object BarcodeFoodScanner {

    fun startScan(
        context: Context,
        onBarcodeScanned: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_QR_CODE
            )
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(context, options)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (!rawValue.isNullOrBlank()) {
                    onBarcodeScanned(rawValue)
                } else {
                    onError("No barcode recognized")
                }
            }
            .addOnCanceledListener {
                // User dismissed the scanner UI
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Barcode scanning unavailable")
            }
    }
}
