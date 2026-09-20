package com.mopay.customer

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val bg = Color.rgb(247, 247, 247)
    private val black = Color.rgb(17, 17, 17)
    private val yellow = Color.rgb(255, 204, 0)
    private val darkGray = Color.rgb(70, 70, 70)
    private val green = Color.rgb(20, 150, 85)
    private val red = Color.rgb(210, 55, 55)
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var currentCardStatus = "active"
    private var lastSnapshot: JSONObject? = null

    private val refreshRunnable = object : Runnable {
        override fun run() { loadSnapshot(false); handler.postDelayed(this, 5000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = black
        loadSnapshot(true)
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshRunnable)
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun loadSnapshot(initial: Boolean) {
        executor.execute {
            try {
                val data = SupabaseClient.rpc("get_demo_customer_snapshot")
                runOnUiThread {
                    lastSnapshot = data
                    currentCardStatus = data.optJSONObject("card")?.optString("status", "active") ?: "active"
                    render(data)
                    if (initial) handler.postDelayed(refreshRunnable, 5000)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (initial) renderError(e.message ?: "Connection error")
                    else Toast.makeText(this, "Sync failed — retrying…", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun renderError(message: String) {
        val root = baseRoot()
        root.addView(text("MoPay", 30f, Color.WHITE, Typeface.BOLD).apply { setPadding(dp(22), dp(28), dp(22), 4) })
        root.addView(text("CUSTOMER WALLET", 12f, Color.LTGRAY, Typeface.BOLD).apply { setPadding(dp(22), 0, dp(22), dp(20)) })
        root.addView(card(black, dp(18)).apply {
            setPadding(dp(18), dp(18), dp(18), dp(18))
            addView(text("Unable to connect", 20f, Color.WHITE, Typeface.BOLD))
            addView(text(message, 14f, Color.LTGRAY).apply { setPadding(0, dp(8), 0, dp(16)) })
            addView(button("RETRY", yellow, black) { loadSnapshot(true) })
        }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(18), dp(30), dp(18), 0) })
        setContentView(root)
    }

    private fun render(data: JSONObject) {
        val root = baseRoot()
        val customer = data.optJSONObject("customer") ?: JSONObject()
        val wallet = data.optJSONObject("wallet") ?: JSONObject()
        val card = data.optJSONObject("card") ?: JSONObject()
        val balance = wallet.optLong("balance", 0)
        val currency = wallet.optString("currency", "XAF")
        val fullName = customer.optString("full_name", "NGOH ERAN")
        val masked = card.optString("masked_number", "•••• 4821")
        val frozen = currentCardStatus != "active"

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(16), dp(18), dp(18)) }
        scroll.addView(content)

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(ImageView(this).apply { setImageResource(R.drawable.ic_mopay_logo) }, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0)
            addView(text("Hello,", 13f, darkGray)); addView(text(fullName, 18f, black, Typeface.BOLD)); addView(text("Customer", 12f, Color.GRAY))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button("◔", Color.TRANSPARENT, black) { showProfile() }, LinearLayout.LayoutParams(dp(50), dp(50)))
        content.addView(header)

        val balanceCard = card(yellow, dp(22)).apply { setPadding(dp(20), dp(18), dp(20), dp(18)) }
        val balanceTop = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        balanceTop.addView(text("Total Balance", 14f, black, Typeface.BOLD), LinearLayout.LayoutParams(0, -2, 1f))
        balanceTop.addView(pill("MTN", black, yellow))
        balanceCard.addView(balanceTop)
        balanceCard.addView(text(formatMoney(balance) + " FCFA", 34f, black, Typeface.BOLD).apply { setPadding(0, dp(6), 0, 0) })
        val balanceBottom = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        balanceBottom.addView(text("Available balance", 12f, darkGray), LinearLayout.LayoutParams(0, -2, 1f))
        balanceBottom.addView(text(currency, 12f, darkGray, Typeface.BOLD))
        balanceCard.addView(balanceBottom)
        content.addView(balanceCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })

        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val actionData = listOf("↑" to "Top Up", "➤" to "Send", "▣" to "Pay", "•••" to "More")
        val actionFns = listOf<() -> Unit>({ showTopUp() }, { showSend() }, { showPay() }, { showMore() })
        actionData.forEachIndexed { index, pair ->
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(8), dp(10), dp(8), dp(10)); background = rounded(Color.WHITE, dp(16)); elevation = dp(2).toFloat()
                setOnClickListener { actionFns[index]() }
                isClickable = true
            }
            box.addView(text(pair.first, 23f, black, Typeface.BOLD).apply { gravity = Gravity.CENTER })
            box.addView(text(pair.second, 11f, darkGray, Typeface.BOLD).apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) })
            actions.addView(box, LinearLayout.LayoutParams(0, dp(82), 1f).apply { setMargins(dp(4), dp(14), dp(4), 0) })
        }
        content.addView(actions)

        val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(text("My Card", 18f, black, Typeface.BOLD), LinearLayout.LayoutParams(0, -2, 1f))
        titleRow.addView(button("View Details ›", Color.TRANSPARENT, Color.rgb(180, 135, 0)) { showCardDetails() }, LinearLayout.LayoutParams(dp(125), dp(45)))
        content.addView(titleRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) })

        val physicalCard = card(black, dp(18)).apply { setPadding(dp(20), dp(18), dp(20), dp(18)) }
        val cardBrand = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        cardBrand.addView(text("MoPay", 25f, Color.WHITE, Typeface.BOLD), LinearLayout.LayoutParams(0, -2, 1f))
        cardBrand.addView(text(")))", 20f, yellow, Typeface.BOLD))
        cardBrand.addView(pill("NFC", yellow, black).apply { setPadding(dp(10), dp(5), dp(10), dp(5)) })
        physicalCard.addView(cardBrand)
        physicalCard.addView(text(masked, 25f, Color.WHITE).apply { letterSpacing = 0.08f; setPadding(0, dp(18), 0, dp(5)) })
        physicalCard.addView(text(fullName, 14f, Color.WHITE, Typeface.BOLD))
        val cardBottom = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        cardBottom.addView(text(if (frozen) "● FROZEN" else "● ACTIVE", 12f, if (frozen) Color.rgb(255, 100, 100) else Color.rgb(55, 220, 125), Typeface.BOLD), LinearLayout.LayoutParams(0, -2, 1f))
        cardBottom.addView(text("Valid Thru 12/28", 11f, Color.LTGRAY))
        physicalCard.addView(cardBottom)
        content.addView(physicalCard, LinearLayout.LayoutParams(-1, dp(158)).apply { topMargin = dp(10) })

        val freezeRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        freezeRow.addView(button(if (frozen) "UNFREEZE CARD" else "FREEZE CARD", if (frozen) Color.WHITE else yellow, black) { changeCardStatus(if (frozen) "active" else "frozen") }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(0, dp(10), dp(5), 0) })
        freezeRow.addView(button("CARD SETTINGS", Color.WHITE, black) { showCardSettings() }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(dp(5), dp(10), 0, 0) })
        content.addView(freezeRow)

        val transactionHeader = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        transactionHeader.addView(text("Recent Transactions", 18f, black, Typeface.BOLD), LinearLayout.LayoutParams(0, -2, 1f))
        transactionHeader.addView(button("View All ›", Color.TRANSPARENT, Color.rgb(180, 135, 0)) { showTransactions() }, LinearLayout.LayoutParams(dp(95), dp(45)))
        content.addView(transactionHeader, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22) })

        val txns = data.optJSONArray("transactions") ?: JSONArray()
        if (txns.length() == 0) content.addView(text("No transactions yet.", 14f, Color.GRAY).apply { setPadding(dp(6), dp(14), 0, dp(12)) })
        else for (i in 0 until minOf(txns.length(), 5)) content.addView(transactionRow(txns.getJSONObject(i)))

        val bottom = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(2)) }
        bottom.addView(navItem("⌂", "Home") { loadSnapshot(false) }, LinearLayout.LayoutParams(0, dp(70), 1f))
        bottom.addView(navItem("▤", "Transactions") { showTransactions() }, LinearLayout.LayoutParams(0, dp(70), 1f))
        bottom.addView(navItem("▦", "Card") { showCardDetails() }, LinearLayout.LayoutParams(0, dp(70), 1f))
        bottom.addView(navItem("♙", "Profile") { showProfile() }, LinearLayout.LayoutParams(0, dp(70), 1f))
        content.addView(bottom)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun showTopUp() {
        val input = amountEdit("Amount to add")
        AlertDialog.Builder(this).setTitle("Top Up Demo Wallet").setMessage("Demo only — this changes the Supabase demo balance.").setView(input)
            .setNegativeButton("CANCEL", null).setPositiveButton("TOP UP") { _, _ ->
                val amount = input.text.toString().toLongOrNull() ?: 0
                if (amount <= 0) Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show() else adjustWallet("demo_top_up", JSONObject().put("p_amount", amount), "Top up successful")
            }.show()
    }

    private fun showSend() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(4), 0, dp(4), 0) }
        val recipient = EditText(this).apply { hint = "Recipient phone"; inputType = InputType.TYPE_CLASS_PHONE }
        val amount = amountEdit("Amount")
        box.addView(recipient); box.addView(amount)
        AlertDialog.Builder(this).setTitle("Send Money — Demo").setMessage("Demo transfer only.").setView(box)
            .setNegativeButton("CANCEL", null).setPositiveButton("SEND") { _, _ ->
                val value = amount.text.toString().toLongOrNull() ?: 0
                val phone = recipient.text.toString().trim()
                if (value <= 0 || phone.isBlank()) Toast.makeText(this, "Enter recipient and amount", Toast.LENGTH_SHORT).show()
                else adjustWallet("demo_send", JSONObject().put("p_amount", value).put("p_recipient", phone), "Transfer successful")
            }.show()
    }

    private fun showPay() {
        AlertDialog.Builder(this).setTitle("Pay with MoPay")
            .setMessage("For this demo, open MoPay Merchant on the merchant phone, enter the amount, then tap the MoPay NFC demo card/Android HCE device.")
            .setPositiveButton("OK", null).show()
    }

    private fun showMore() {
        val items = arrayOf("Transaction History", "Card Settings", "Profile", "Refresh from Supabase")
        AlertDialog.Builder(this).setTitle("More").setItems(items) { _, which -> when (which) {
            0 -> showTransactions(); 1 -> showCardSettings(); 2 -> showProfile(); 3 -> loadSnapshot(false)
        }}.show()
    }

    private fun showCardDetails() {
        val c = lastSnapshot?.optJSONObject("card") ?: JSONObject()
        AlertDialog.Builder(this).setTitle("Card Details")
            .setMessage("Card: ${c.optString("masked_number", "•••• 4821")}\nStatus: ${c.optString("status", currentCardStatus).uppercase()}\nType: Secure NFC demo card\nLast used: ${c.optString("last_used_at", "Not used yet")}")
            .setPositiveButton("OK", null).show()
    }

    private fun showCardSettings() {
        val options = arrayOf(if (currentCardStatus == "active") "Freeze card" else "Unfreeze card", "Change PIN", "Card details")
        AlertDialog.Builder(this).setTitle("Card Settings").setItems(options) { _, which -> when (which) {
            0 -> changeCardStatus(if (currentCardStatus == "active") "frozen" else "active")
            1 -> showChangePin()
            2 -> showCardDetails()
        }}.show()
    }

    private fun showChangePin() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(6), 0, dp(6), 0) }
        val oldPin = pinEdit("Current PIN"); val newPin = pinEdit("New 4-digit PIN"); val confirm = pinEdit("Confirm new PIN")
        box.addView(oldPin); box.addView(newPin); box.addView(confirm)
        AlertDialog.Builder(this).setTitle("Change PIN").setView(box).setNegativeButton("CANCEL", null).setPositiveButton("CHANGE") { _, _ ->
            val n = newPin.text.toString(); val c = confirm.text.toString()
            if (oldPin.text.length != 4 || n.length != 4 || n != c) { Toast.makeText(this, "Check the PIN fields", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            executor.execute {
                try {
                    val result = SupabaseClient.rpc("change_demo_pin", JSONObject().put("p_card_token", "CARD_DEMO_4821").put("p_old_pin", oldPin.text.toString()).put("p_new_pin", n))
                    runOnUiThread { Toast.makeText(this, if (result.optBoolean("success")) "PIN changed" else "PIN change failed", Toast.LENGTH_SHORT).show() }
                } catch (_: Exception) { runOnUiThread { Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show() } }
            }
        }.show()
    }

    private fun showTransactions() {
        val txns = lastSnapshot?.optJSONArray("transactions") ?: JSONArray()
        val lines = StringBuilder()
        if (txns.length() == 0) lines.append("No transactions yet.")
        for (i in 0 until txns.length()) {
            val t = txns.getJSONObject(i)
            lines.append(if (t.optString("status") == "successful") "✓ " else "× ")
                .append(formatMoney(t.optLong("amount"))).append(" FCFA — ")
                .append(t.optString("status").uppercase()).append("\n")
                .append(t.optString("merchant_name", "MoPay Merchant")).append(" • ")
                .append(formatDate(t.optString("created_at"))).append("\n\n")
        }
        AlertDialog.Builder(this).setTitle("Transaction History").setMessage(lines.toString()).setPositiveButton("CLOSE", null).setNeutralButton("REFRESH") { _, _ -> loadSnapshot(false) }.show()
    }

    private fun showProfile() {
        val c = lastSnapshot?.optJSONObject("customer") ?: JSONObject()
        AlertDialog.Builder(this).setTitle("Profile").setMessage("Name: ${c.optString("full_name", "NGOH ERAN")}\nPhone: ${c.optString("phone_number", "—")}\nWallet: Demo XAF\nAccount status: Active").setPositiveButton("CLOSE", null).show()
    }

    private fun adjustWallet(functionName: String, body: JSONObject, successText: String) {
        executor.execute {
            try {
                val result = SupabaseClient.rpc(functionName, body)
                runOnUiThread { Toast.makeText(this, if (result.optBoolean("success")) successText else humanReason(result.optString("reason")), Toast.LENGTH_SHORT).show(); loadSnapshot(false) }
            } catch (_: Exception) { runOnUiThread { Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show() } }
        }
    }

    private fun changeCardStatus(status: String) {
        executor.execute {
            try {
                val result = SupabaseClient.rpc("set_demo_card_status", JSONObject().put("p_card_token", "CARD_DEMO_4821").put("p_status", status))
                runOnUiThread { Toast.makeText(this, if (result.optBoolean("success")) if (status == "frozen") "Card frozen" else "Card unfrozen" else "Unable to change card status", Toast.LENGTH_SHORT).show(); loadSnapshot(false) }
            } catch (_: Exception) { runOnUiThread { Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show() } }
        }
    }

    private fun humanReason(reason: String): String = when (reason) { "INSUFFICIENT_FUNDS" -> "Insufficient balance"; "INVALID_AMOUNT" -> "Invalid amount"; else -> reason.replace('_', ' ').ifBlank { "Action failed" } }

    private fun transactionRow(t: JSONObject): View {
        val successful = t.optString("status") == "successful"
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4), dp(10), dp(4), dp(10)) }
        row.addView(text(if (successful) "✓" else "×", 18f, if (successful) green else red, Typeface.BOLD).apply { gravity = Gravity.CENTER; background = rounded(if (successful) Color.rgb(230, 248, 238) else Color.rgb(255, 235, 235), dp(28)) }, LinearLayout.LayoutParams(dp(42), dp(42)))
        row.addView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, dp(4), 0); addView(text("Payment", 14f, black, Typeface.BOLD)); addView(text(t.optString("merchant_name", "MoPay Merchant"), 11f, Color.GRAY)); addView(text(formatDate(t.optString("created_at")), 10f, Color.GRAY)) }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text((if (successful) "- " else "") + formatMoney(t.optLong("amount")) + " FCFA", 13f, black, Typeface.BOLD))
        return row
    }

    private fun navItem(icon: String, label: String, action: () -> Unit): View = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setOnClickListener { action() }; addView(text(icon, 24f, black, Typeface.BOLD)); addView(text(label, 10f, darkGray, Typeface.BOLD)) }
    private fun button(label: String, fill: Int, foreground: Int, action: () -> Unit): Button = Button(this).apply { text = label; textSize = 12f; setTextColor(foreground); typeface = Typeface.DEFAULT_BOLD; isAllCaps = false; background = rounded(fill, dp(14)); setOnClickListener { action() } }
    private fun amountEdit(hintText: String) = EditText(this).apply { hint = hintText; inputType = InputType.TYPE_CLASS_NUMBER; setPadding(dp(10), dp(8), dp(10), dp(8)) }
    private fun pinEdit(hintText: String) = EditText(this).apply { hint = hintText; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD; setPadding(dp(10), dp(8), dp(10), dp(8)) }
    private fun pill(label: String, fill: Int, foreground: Int): TextView = text(label, 10f, foreground, Typeface.BOLD).apply { gravity = Gravity.CENTER; setPadding(dp(8), dp(4), dp(8), dp(4)); background = rounded(fill, dp(20)) }
    private fun text(value: String, size: Float, color: Int, style: Int = Typeface.NORMAL) = TextView(this).apply { text = value; textSize = size; setTextColor(color); typeface = Typeface.create("sans", style) }
    private fun rounded(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat() }
    private fun card(color: Int, radius: Int): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(color, radius); elevation = dp(2).toFloat() }
    private fun baseRoot() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
    private fun formatMoney(value: Long): String = NumberFormat.getNumberInstance(Locale.US).format(value)
    private fun formatDate(value: String): String = try { val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US); SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(parser.parse(value) ?: Date()) } catch (_: Exception) { value.take(16).replace('T', ' ') }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
