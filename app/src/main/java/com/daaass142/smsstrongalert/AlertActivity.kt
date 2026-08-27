package com.daaass142.smsstrongalert

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertActivity : Activity() {
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            ) { acknowledge() }
        }

        buildUi()
        render(intent.extras)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(intent.extras)
    }

    @SuppressLint("GestureBackNavigation")
    @Deprecated("Legacy back callback for Android 12L and older")
    override fun onBackPressed() {
        acknowledge()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(215, 0, 35))
            isFillViewport = true
        }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(72), dp(24), dp(36))
        }

        scroll.addView(
            root,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        setContentView(scroll)
    }

    private fun render(bundle: Bundle?) {
        AlertNotifier.cancel(this)
        root.removeAllViews()

        val body = bundle?.getString(AlertNotifier.EXTRA_BODY).orEmpty()
        val sender = bundle?.getString(AlertNotifier.EXTRA_SENDER).orEmpty()
        val plate = bundle?.getString(AlertNotifier.EXTRA_PLATE).orEmpty()
        val receivedAt = bundle?.getLong(AlertNotifier.EXTRA_RECEIVED_AT)
            ?.takeIf { it > 0 }
            ?: System.currentTimeMillis()

        val durationMs = Prefs.durationSeconds(this) * 1000L
        AlertSoundController.start(this, durationMs)

        addText("🚨", 58f, Color.WHITE, true)
        addText("呼叫！呼叫！", 28f, Color.WHITE, true, top = 8)
        addText(
            SimpleDateFormat("yyyy年M月d日 HH:mm:ss", Locale.CHINA).format(Date(receivedAt)),
            14f,
            0xFFFFE5E8.toInt(),
            false,
            top = 8,
        )

        if (plate.isNotBlank()) {
            addCardText(plate, 30f, Color.WHITE, top = 24)
        }

        addCardText(
            body.ifBlank { "检测到疑似违停短信，请尽快确认并处理。" },
            17f,
            Color.WHITE,
            top = 24,
        )

        addText(
            "发送号码：${sender.ifBlank { "未知" }}",
            13f,
            0xFFFFE5E8.toInt(),
            false,
            top = 16,
        )

        val button = Button(this).apply {
            text = "收到，马上去处理"
            textSize = 18f
            isAllCaps = false
            setTextColor(0xFFD70023.toInt())
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            setOnClickListener { acknowledge() }
        }
        root.addView(button, lp(top = 28, height = 56))

        addText("短信叫叫叫 · 本地识别 · 无网络权限", 12f, 0xFFFFD1D8.toInt(), false, top = 18)
    }

    private fun acknowledge() {
        AlertNotifier.cancel(this)
        AlertSoundController.stop()
        finishAndRemoveTask()
    }

    private fun addText(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean,
        top: Int = 0,
    ) {
        val view = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
        root.addView(view, lp(top = top))
    }

    private fun addCardText(text: String, size: Float, color: Int, top: Int) {
        val view = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(16), dp(20), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(0x33FFFFFF)
                setStroke(dp(1), 0x99FFFFFF.toInt())
            }
        }
        root.addView(view, lp(top = top))
    }

    private fun lp(top: Int = 0, height: Int = LinearLayout.LayoutParams.WRAP_CONTENT): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            if (height == LinearLayout.LayoutParams.WRAP_CONTENT) height else dp(height),
        ).apply {
            topMargin = dp(top)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
