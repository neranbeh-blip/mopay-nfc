package com.mopay.customer

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("demo_wallet", Context.MODE_PRIVATE) }
    private var balance = 100000L
    private var frozen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        balance = prefs.getLong("balance", 100000L)
        frozen = prefs.getBoolean("frozen", false)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 50, 36, 36)
            setBackgroundColor(Color.rgb(248, 250, 253))
        }

        root.addView(TextView(this).apply {
            text = "MoPay"
            textSize = 30f
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(Color.rgb(10, 25, 45))
        })

        root.addView(TextView(this).apply {
            text = "CUSTOMER WALLET"
            textSize = 13f
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(Color.rgb(80, 100, 120))
        })

        root.addView(TextView(this).apply {
            text = "%,d FCFA".format(balance)
            textSize = 38f
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 45, 0, 25)
            setTextColor(Color.rgb(11, 75, 125))
        })

        root.addView(TextView(this).apply {
            text = if (frozen) "CARD FROZEN" else "CARD ACTIVE"
            textSize = 17f
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 10, 0, 35)
            setTextColor(if (frozen) Color.rgb(190, 50, 50) else Color.rgb(20, 130, 80))
        })

        root.addView(TextView(this).apply {
            text = "NFC CARD\n\nNGOH ERAN\nCARD •••• 4821\nNFC"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(20, 30, 20, 30)
            setBackgroundColor(Color.rgb(12, 30, 52))
        })

        root.addView(Button(this).apply {
            text = if (frozen) "UNFREEZE CARD" else "FREEZE CARD"
            setOnClickListener {
                frozen = !frozen
                prefs.edit().putBoolean("frozen", frozen).apply()
                render()
            }
        }, LinearLayout.LayoutParams(-1, 58).apply { topMargin = 30 })

        root.addView(TextView(this).apply {
            text = "Recent transactions\n\nNo transactions yet in this local prototype."
            textSize = 16f
            setPadding(0, 30, 0, 0)
            setTextColor(Color.DKGRAY)
        })

        setContentView(root)
    }
}
