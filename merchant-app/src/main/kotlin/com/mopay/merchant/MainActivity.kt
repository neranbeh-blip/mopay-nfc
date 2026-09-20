package com.mopay.merchant

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.Charset
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val bg = Color.rgb(247, 247, 247)
    private val black = Color.rgb(17, 17, 17)
    private val yellow = Color.rgb(255, 204, 0)
    private val green = Color.rgb(20, 150, 85)
    private val red = Color.rgb(210, 55, 55)
    private val darkGray = Color.rgb(70, 70, 70)
    private val executor = Executors.newSingleThreadExecutor()

    private var nfcAdapter: NfcAdapter? = null
    private var detectedCardToken = ""
    private var dashboard = JSONObject()
    private lateinit var amountInput: EditText
    private lateinit var cardStatus: TextView
    private lateinit var terminalTitle: TextView
    private lateinit var terminalSub: TextView
    private lateinit var contentHost: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = black
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        loadDashboard()
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

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun loadDashboard() {
        executor.execute {
            try {
                val data = SupabaseClient.rpc("get_demo_merchant_dashboard")
                runOnUiThread { dashboard = data; render() }
            } catch (e: Exception) {
                runOnUiThread { render(); Toast.makeText(this, "Supabase sync unavailable", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        contentHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        scroll.addView(contentHost)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        val merchant = dashboard.optJSONObject("merchant") ?: JSONObject()
        val businessName = merchant.optString("business_name", "MoPay Store")
        val today = dashboard.optJSONObject("today") ?: JSONObject()
        val count = today.optInt("transaction_count", 0)
        val success = today.optInt("successful_count", 0)
        val failed = today.optInt("failed_count", 0)
        val total = today.optLong("total_amount", 0)

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val logo = ImageView(this).apply { setImageResource(com.mopay.merchant.R.drawable.ic_mopay_logo) }
        header.addView(logo, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            addView(text("MoPay", 22f, black, Typeface.BOLD))
            addView(text("Merchant", 12f, Color.rgb(190, 145, 0), Typeface.BOLD))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(text("☰", 26f, black))
        contentHost.addView(header)

        // NFC terminal hero
        val hero = card(black, dp(22))
        hero.setPadding(dp(20), dp(20), dp(20), dp(20))
        val heroTop = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        heroTop.addView(text("NFC PAYMENT TERMINAL", 16f, Color.WHITE, Typeface.BOLD), LinearLayout.LayoutParams(0, -2, 1f))
        heroTop.addView(pill("MTN", yellow, black))
        hero.addView(heroTop)
        terminalTitle = text(if (detectedCardToken.isBlank()) "Ready to accept payment" else "CARD DETECTED", 14f, Color.WHITE, Typeface.BOLD).apply { gravity = Gravity.CENTER; setPadding(0, dp(18), 0, dp(8)) }
        hero.addView(terminalTitle)
        hero.addView(text("◉", 58f, yellow, Typeface.BOLD).apply { gravity = Gravity.CENTER })
        terminalSub = text(if (nfcAdapter == null) "NFC not available on this device" else "Bring customer's card near the back of this phone", 12f, Color.LTGRAY).apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) }
        hero.addView(terminalSub)
        cardStatus = text(if (detectedCardToken.isBlank()) "Waiting for NFC card…" else "Card ready", 12f, yellow, Typeface.BOLD).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, 0) }
        hero.addView(cardStatus)
        contentHost.addView(hero, LinearLayout.LayoutParams(-1, dp(220)).apply { topMargin = dp(16) })

        // Amount
        val amountLabel = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        amountLabel.addView(text("Enter Amount", 15f, black, Typeface.BOLD), LinearLayout.LayoutParams(0, -2, 1f))
        amountLabel.addView(text("XAF", 12f, Color.GRAY, Typeface.BOLD))
        contentHost.addView(amountLabel, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })

        amountInput = EditText(this).apply {
            hint = "0"
            textSize = 31f
            setTextColor(black)
            setHintTextColor(Color.LTGRAY)
            inputType = InputType.TYPE_CLASS_NUMBER
            setPadding(dp(16), dp(5), dp(16), dp(5))
            background = rounded(Color.WHITE, dp(15))
        }
        contentHost.addView(amountInput, LinearLayout.LayoutParams(-1, dp(66)).apply { topMargin = dp(8) })

        val quick = LinearLayout(this).apply { gravity = Gravity.CENTER }
        listOf("1,000", "2,000", "5,000", "10,000").forEach { value ->
            quick.addView(button(value, Color.WHITE, black) { amountInput.setText(value.replace(",", "")); amountInput.setSelection(amountInput.length()) }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(3), dp(8), dp(3), 0) })
        }
        contentHost.addView(quick)

        contentHost.addView(button("Proceed to Payment   →", yellow, black) { proceedToPayment() }, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(10) })

        // Daily summary
        contentHost.addView(text("Today's Summary", 17f, black, Typeface.BOLD), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22) })
        val summary = LinearLayout(this).apply { gravity = Gravity.CENTER; background = rounded(Color.WHITE, dp(18)); setPadding(dp(6), dp(14), dp(6), dp(14)) }
        summary.addView(stat("▤", count.toString(), "Transactions"), LinearLayout.LayoutParams(0, dp(86), 1f))
        summary.addView(stat("◉", formatMoney(total), "Total Amount"), LinearLayout.LayoutParams(0, dp(86), 1f))
        summary.addView(stat("✓", success.toString(), "Successful"), LinearLayout.LayoutParams(0, dp(86), 1f))
        summary.addView(stat("×", failed.toString(), "Failed"), LinearLayout.LayoutParams(0, dp(86), 1f))
        contentHost.addView(summary, LinearLayout.LayoutParams(-1, dp(116)).apply { topMargin = dp(8) })

        contentHost.addView(text("Recent Transactions", 17f, black, Typeface.BOLD), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22) })
        val txns = dashboard.optJSONArray("transactions") ?: JSONArray()
        if (txns.length() == 0) contentHost.addView(text("No transactions today.", 14f, Color.GRAY).apply { setPadding(dp(6), dp(14), 0, dp(14)) })
        else for (i in 0 until minOf(txns.length(), 6)) contentHost.addView(merchantTransaction(txns.getJSONObject(i)))

        val nav = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(4))
            addView(navItem("⌂", "Home", true), LinearLayout.LayoutParams(0, dp(70), 1f))
            addView(navItem("▤", "Transactions", false), LinearLayout.LayoutParams(0, dp(70), 1f))
            addView(navItem("◉", "Scan", true), LinearLayout.LayoutParams(0, dp(70), 1f))
            addView(navItem("▥", "Reports", false), LinearLayout.LayoutParams(0, dp(70), 1f))
            addView(navItem("⚙", "Settings", false), LinearLayout.LayoutParams(0, dp(70), 1f))
        }
        contentHost.addView(nav)
    }

    private fun proceedToPayment() {
        val amount = amountInput.text.toString().toLongOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        if (detectedCardToken.isBlank()) {
            Toast.makeText(this, "Tap a customer NFC card first", Toast.LENGTH_SHORT).show()
            return
        }

        val pin = EditText(this).apply {
            hint = "4-digit PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 24f
            gravity = Gravity.CENTER
        }
        AlertDialog.Builder(this)
            .setTitle("Authorize Payment")
            .setMessage("Charge %,d FCFA to card •••• 4821".format(amount))
            .setView(pin)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("PAY", null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val entered = pin.text.toString()
                        if (entered.length != 4) {
                            pin.error = "Enter 4 digits"
                            return@setOnClickListener
                        }
                        dialog.dismiss()
                        processPayment(amount, entered)
                    }
                }
                dialog.show()
            }
    }

    private fun processPayment(amount: Long, pin: String) {
        cardStatus.text = "Processing secure payment…"
        executor.execute {
            try {
                val body = JSONObject()
                    .put("p_card_token", detectedCardToken)
                    .put("p_merchant_id", "22222222-2222-2222-2222-222222222222")
                    .put("p_amount", amount)
                    .put("p_pin", pin)
                val result = SupabaseClient.rpc("process_demo_payment", body)
                runOnUiThread {
                    if (result.optBoolean("success")) {
                        val newBalance = result.optLong("balance_after", 0)
                        AlertDialog.Builder(this)
                            .setTitle("Payment Successful")
                            .setMessage("%,d FCFA paid\nReference: %s\nCustomer balance: %,d FCFA".format(amount, result.optString("reference"), newBalance))
                            .setPositiveButton("DONE") { _, _ -> loadDashboard() }
                            .show()
                        amountInput.setText("")
                    } else {
                        val reason = result.optString("reason", "PAYMENT_DECLINED")
                        cardStatus.text = "PAYMENT DECLINED"
                        AlertDialog.Builder(this)
                            .setTitle("Payment Declined")
                            .setMessage(humanReason(reason))
                            .setPositiveButton("OK") { _, _ -> loadDashboard() }
                            .show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    cardStatus.text = "CONNECTION ERROR"
                    Toast.makeText(this, "Supabase connection error", Toast.LENGTH_LONG).show()
                }
            }
        }
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
            } else null
        } catch (_: Exception) { null }

        // Demo fallback: any NFC tag can represent the demo card if no NDEF text exists.
        detectedCardToken = text?.takeIf { it.isNotBlank() } ?: "CARD_DEMO_4821"
        terminalTitle.text = "CARD DETECTED"
        terminalSub.text = "Card •••• 4821 — ready for payment"
        cardStatus.text = "✓ CARD READY — ENTER AMOUNT"
    }

    private fun decodePayload(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val languageLength = payload[0].toInt() and 0x3F
        val start = 1 + languageLength
        return if (start < payload.size) String(payload, start, payload.size - start, Charset.forName("UTF-8")) else String(payload, Charset.forName("UTF-8"))
    }

    private fun humanReason(reason: String): String = when (reason) {
        "CARD_FROZEN" -> "This card is frozen. Unfreeze it in the Customer app and try again."
        "CARD_BLOCKED" -> "This card is blocked."
        "CARD_LOST" -> "This card is marked as lost."
        "INVALID_PIN" -> "The PIN is incorrect."
        "INSUFFICIENT_FUNDS" -> "The customer does not have enough balance."
        "CARD_NOT_FOUND" -> "Card not recognized by MoPay."
        else -> reason.replace('_', ' ')
    }

    private fun merchantTransaction(t: JSONObject): TextView {
        val successful = t.optString("status") == "successful"
        return text(
            "${if (successful) "✓" else "×"}  ${formatMoney(t.optLong("amount"))} FCFA     ${if (successful) "Success" else "Failed"}\n" +
                "    Card ${t.optString("card_number", "•••• 4821")}   ${formatDate(t.optString("created_at"))}",
            12f, if (successful) black else red
        ).apply {
            setPadding(dp(8), dp(10), dp(8), dp(10))
            background = rounded(Color.WHITE, dp(12))
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(-1, dp(62)).apply { setMargins(0, dp(6), 0, 0) }
        }
    }

    private fun stat(icon: String, value: String, label: String): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        box.addView(text(icon, 20f, black, Typeface.BOLD).apply { gravity = Gravity.CENTER })
        box.addView(text(value, 13f, black, Typeface.BOLD).apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) })
        box.addView(text(label, 9f, Color.GRAY).apply { gravity = Gravity.CENTER })
        return box
    }

    private fun navItem(icon: String, label: String, selected: Boolean): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        box.addView(text(icon, 23f, if (selected) Color.rgb(205, 155, 0) else Color.GRAY, Typeface.BOLD))
        box.addView(text(label, 9f, if (selected) black else Color.GRAY, Typeface.BOLD))
        return box
    }

    private fun card(color: Int, radius: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(color, radius)
        elevation = dp(2).toFloat()
    }

    private fun button(label: String, fill: Int, foreground: Int, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 12f
        setTextColor(foreground)
        typeface = Typeface.DEFAULT_BOLD
        isAllCaps = false
        background = rounded(fill, dp(14))
        setOnClickListener { action() }
    }

    private fun pill(label: String, fill: Int, foreground: Int) = text(label, 10f, foreground, Typeface.BOLD).apply {
        gravity = Gravity.CENTER
        setPadding(dp(9), dp(5), dp(9), dp(5))
        background = rounded(fill, dp(20))
    }

    private fun text(value: String, size: Float, color: Int, style: Int = Typeface.NORMAL) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        typeface = Typeface.create("sans", style)
    }

    private fun rounded(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat() }
    private fun formatMoney(value: Long): String = NumberFormat.getNumberInstance(Locale.US).format(value)
    private fun formatDate(value: String): String = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(parser.parse(value) ?: Date())
    } catch (_: Exception) { value.take(16).replace('T', ' ') }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
