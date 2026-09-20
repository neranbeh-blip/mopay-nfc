package com.mopay.customer

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
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
    private val lightGray = Color.rgb(235, 235, 235)
    private val green = Color.rgb(20, 150, 85)
    private val red = Color.rgb(210, 55, 55)

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var currentCardStatus = "active"
    private var lastSnapshot: JSONObject? = null

    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadSnapshot(false)
            handler.postDelayed(this, 5000)
        }
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
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(18))
        }
        scroll.addView(content)

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val logo = ImageView(this).apply { setImageResource(com.mopay.customer.R.drawable.ic_mopay_logo) }
        header.addView(logo, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            addView(text("Hello,", 13f, darkGray))
            addView(text(fullName, 18f, black, Typeface.BOLD))
            addView(text("Customer", 12f, Color.GRAY))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(text("◔", 26f, black).apply { gravity = Gravity.CENTER })
        content.addView(header)

        // Balance card
        val balanceCard = card(yellow, dp(22))
        balanceCard.setPadding(dp(20), dp(18), dp(20), dp(18))
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

        // Quick actions
        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER }
        listOf("↑" to "Top Up", "➤" to "Send", "▣" to "Pay", "•••" to "More").forEach { (icon, label) ->
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(10), dp(8), dp(10))
                background = rounded(Color.WHITE, dp(16))
                elevation = dp(2).toFloat()
            }
            box.addView(text(icon, 23f, black, Typeface.BOLD).apply { gravity = Gravity.CENTER })
            box.addView(text(label, 11f, darkGray, Typeface.BOLD).apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) })
            actions.addView(box, LinearLayout.LayoutParams(0, dp(82), 1f).apply { setMargins(dp(4), dp(14), dp(4), 0) })
        }
        content.addView(actions)

        // Card section
        val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(text("My Card", 18f, black, Typeface.BOLD), LinearLayout.LayoutParams(0, -2, 1f))
        titleRow.addView(text("View Details ›", 13f, Color.rgb(180, 135, 0), Typeface.BOLD))
        content.addView(titleRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20) })

        val physicalCard = card(black, dp(18))
        physicalCard.setPadding(dp(20), dp(18), dp(20), dp(18))
        val cardBrand = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        cardBrand.addView(text("MoPay", 25f, Color.WHITE, Typeface.BOLD), LinearLayout.LayoutParams(0, -2, 1f))
        cardBrand.addView(text(")))", 20f, yellow, Typeface.BOLD))
        cardBrand.addView(pill("NFC", yellow, black).apply { setPadding(dp(10), dp(5), dp(10), dp(5)) })
        physicalCard.addView(cardBrand)
        physicalCard.addView(text(masked, 25f, Color.WHITE, Typeface.NORMAL).apply { letterSpacing = 0.08f; setPadding(0, dp(18), 0, dp(5)) })
        physicalCard.addView(text(fullName, 14f, Color.WHITE, Typeface.BOLD))
        val cardBottom = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        cardBottom.addView(text(if (frozen) "● FROZEN" else "● ACTIVE", 12f, if (frozen) Color.rgb(255, 100, 100) else Color.rgb(55, 220, 125), Typeface.BOLD), LinearLayout.LayoutParams(0, -2, 1f))
        cardBottom.addView(text("Valid Thru 12/28", 11f, Color.LTGRAY))
        physicalCard.addView(cardBottom)
        content.addView(physicalCard, LinearLayout.LayoutParams(-1, dp(158)).apply { topMargin = dp(10) })

        // Freeze control
        val freezeRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        freezeRow.addView(button(if (frozen) "UNFREEZE CARD" else "FREEZE CARD", if (frozen) Color.WHITE else yellow, black) {
            changeCardStatus(if (frozen) "active" else "frozen")
        }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(0, dp(10), dp(5), 0) })
        freezeRow.addView(button("CARD SETTINGS", Color.WHITE, black) {
            Toast.makeText(this, "Card settings coming next", Toast.LENGTH_SHORT).show()
        }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(dp(5), dp(10), 0, 0) })
        content.addView(freezeRow)

        // Transactions
        val transactionHeader = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        transactionHeader.addView(text("Recent Transactions", 18f, black, Typeface.BOLD), LinearLayout.LayoutParams(0, -2, 1f))
        transactionHeader.addView(text("View All ›", 13f, Color.rgb(180, 135, 0), Typeface.BOLD))
        content.addView(transactionHeader, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22) })

        val txns = data.optJSONArray("transactions") ?: JSONArray()
        if (txns.length() == 0) {
            content.addView(text("No transactions yet.", 14f, Color.GRAY).apply { setPadding(dp(6), dp(14), 0, dp(12)) })
        } else {
            for (i in 0 until minOf(txns.length(), 5)) {
                content.addView(transactionRow(txns.getJSONObject(i)))
            }
        }

        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(2))
            addView(navItem("⌂", "Home", true), LinearLayout.LayoutParams(0, dp(70), 1f))
            addView(navItem("▤", "Transactions", false), LinearLayout.LayoutParams(0, dp(70), 1f))
            addView(navItem("▦", "Card", false), LinearLayout.LayoutParams(0, dp(70), 1f))
            addView(navItem("♙", "Profile", false), LinearLayout.LayoutParams(0, dp(70), 1f))
        }
        content.addView(bottom)

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun changeCardStatus(status: String) {
        executor.execute {
            try {
                val body = JSONObject().put("p_card_token", "CARD_DEMO_4821").put("p_status", status)
                val result = SupabaseClient.rpc("set_demo_card_status", body)
                runOnUiThread {
                    if (result.optBoolean("success")) {
                        Toast.makeText(this, if (status == "frozen") "Card frozen" else "Card unfrozen", Toast.LENGTH_SHORT).show()
                        loadSnapshot(false)
                    } else Toast.makeText(this, "Unable to change card status", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun transactionRow(t: JSONObject): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(10), dp(4), dp(10))
        }
        val icon = TextView(this).apply {
            text = if (t.optString("status") == "successful") "✓" else "×"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(if (t.optString("status") == "successful") green else red)
            background = rounded(if (t.optString("status") == "successful") Color.rgb(230, 248, 238) else Color.rgb(255, 235, 235), dp(28))
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(42), dp(42)))
        val middle = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, dp(4), 0)
            addView(text("Payment", 14f, black, Typeface.BOLD))
            addView(text(t.optString("merchant_name", "MoPay Merchant"), 11f, Color.GRAY))
            addView(text(formatDate(t.optString("created_at")), 10f, Color.GRAY))
        }
        row.addView(middle, LinearLayout.LayoutParams(0, -2, 1f))
        val amount = t.optLong("amount", 0)
        val successful = t.optString("status") == "successful"
        val right = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            addView(text((if (successful) "- " else "") + formatMoney(amount) + " FCFA", 13f, black, Typeface.BOLD))
            addView(pill(if (successful) "Successful" else "Failed", if (successful) green else red, Color.WHITE))
        }
        row.addView(right)
        return row
    }

    private fun navItem(icon: String, label: String, selected: Boolean): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        box.addView(text(icon, 24f, if (selected) Color.rgb(205, 155, 0) else Color.GRAY, Typeface.BOLD).apply { gravity = Gravity.CENTER })
        box.addView(text(label, 10f, if (selected) black else Color.GRAY, Typeface.BOLD).apply { gravity = Gravity.CENTER })
        return box
    }

    private fun baseRoot() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(bg)
    }

    private fun card(color: Int, radius: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(color, radius)
        elevation = dp(2).toFloat()
    }

    private fun button(label: String, fill: Int, foreground: Int, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 12f
        setTextColor(foreground)
        typeface = Typeface.DEFAULT_BOLD
        isAllCaps = false
        background = rounded(fill, dp(14))
        setOnClickListener { action() }
    }

    private fun pill(label: String, fill: Int, foreground: Int): TextView = text(label, 10f, foreground, Typeface.BOLD).apply {
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(4), dp(8), dp(4))
        background = rounded(fill, dp(20))
    }

    private fun text(value: String, size: Float, color: Int, style: Int = Typeface.NORMAL) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        typeface = Typeface.create("sans", style)
    }

    private fun rounded(color: Int, radius: Int) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }

    private fun formatMoney(value: Long): String = NumberFormat.getNumberInstance(Locale.US).format(value)

    private fun formatDate(value: String): String = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        val date = parser.parse(value)
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(date ?: Date())
    } catch (_: Exception) { value.take(16).replace('T', ' ') }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
