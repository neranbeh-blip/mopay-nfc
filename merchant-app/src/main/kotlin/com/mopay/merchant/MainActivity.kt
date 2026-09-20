package com.mopay.merchant

import android.app.Activity
import android.app.AlertDialog
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.text.InputType
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.nio.charset.Charset

class MainActivity : Activity() {
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var cardView: TextView
    private lateinit var amountInput: EditText
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 50, 36, 36)
            setBackgroundColor(Color.rgb(248, 250, 253))
        }

        root.addView(TextView(this).apply {
            text = "MoPay Merchant"
            textSize = 30f
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(Color.rgb(10, 25, 45))
        })

        root.addView(TextView(this).apply {
            text = "NFC PAYMENT TERMINAL"
            textSize = 13f
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(Color.rgb(80, 100, 120))
        })

        amountInput = EditText(this).apply {
            hint = "Amount (FCFA)"
            inputType = InputType.TYPE_CLASS_NUMBER
            textSize = 20f
        }
        root.addView(amountInput, LinearLayout.LayoutParams(-1, 70).apply { topMargin = 35 })

        cardView = TextView(this).apply {
            text = "TAP CUSTOMER NFC CARD"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(20, 55, 20, 55)
            setTextColor(Color.rgb(11, 75, 125))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(cardView, LinearLayout.LayoutParams(-1, 190).apply { topMargin = 25 })

        statusView = TextView(this).apply {
            text = if (nfcAdapter == null) "NFC NOT AVAILABLE" else "READY — BRING CARD NEAR PHONE"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 20)
        }
        root.addView(statusView)

        root.addView(Button(this).apply {
            text = "SIMULATE PAYMENT"
            setOnClickListener { simulatePayment() }
        }, LinearLayout.LayoutParams(-1, 60))

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            { tag -> runOnUiThread { handleTag(tag) } },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B,
            null
        )
    }

    override fun onPause() {
        nfcAdapter?.disableReaderMode(this)
        super.onPause()
    }

    private fun handleTag(tag: Tag) {
        val text = try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                val message: NdefMessage? = ndef.ndefMessage
                val result = message?.records?.firstOrNull()?.payload?.let { decodePayload(it) }
                ndef.close()
                result
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }

        val cardId = text?.takeIf { it.isNotBlank() } ?: "NFC_TAG_DETECTED"
        cardView.text = "CARD DETECTED\n$cardId"
        statusView.text = "CARD READ — AUTHORIZE PAYMENT"
    }

    private fun decodePayload(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val languageLength = payload[0].toInt() and 0x3F
        val start = 1 + languageLength
        return if (start < payload.size) {
            String(payload, start, payload.size - start, Charset.forName("UTF-8"))
        } else {
            String(payload, Charset.forName("UTF-8"))
        }
    }

    private fun simulatePayment() {
        val amount = amountInput.text.toString().toLongOrNull()
        if (amount == null || amount <= 0) {
            statusView.text = "ENTER A VALID AMOUNT"
            return
        }

        val pin = EditText(this).apply {
            hint = "PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("Authorize Payment")
            .setMessage("Demo payment: %,d FCFA".format(amount))
            .setView(pin)
            .setPositiveButton("PAY") { _, _ ->
                if (pin.text.toString() == "1234") {
                    statusView.text = "✓ PAYMENT SUCCESSFUL\n%,d FCFA".format(amount)
                } else {
                    statusView.text = "✕ PAYMENT DECLINED — INVALID PIN"
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }
}
