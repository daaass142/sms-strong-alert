package com.daaass142.smsstrongalert

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var plateInput: EditText
    private lateinit var durationSpinner: Spinner
    private lateinit var maxVolumeCheck: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFFF5F5F5.toInt())
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(28), dp(18), dp(32))
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "📣 短信叫叫叫"
            textSize = 31f
            setTextColor(0xFF202124.toInt())
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "短信一到，立即强提醒 · 全本地识别 · 不联网"
            textSize = 15f
            setTextColor(0xFF6B6B6B.toInt())
        }, lp(top = 6))

        val statusCard = card()
        statusText = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFF2B2B2B.toInt())
            setLineSpacing(dp(4).toFloat(), 1f)
        }
        statusCard.addView(statusText)

        toggleButton = primaryButton("收工") {
            Prefs.setEnabled(this, !Prefs.isEnabled(this))
            refreshStatus()
        }
        statusCard.addView(toggleButton, lp(top = 18, height = 56))

        statusCard.addView(secondaryButton("授权必要权限") { requestCorePermissions() }, lp(top = 12, height = 50))
        statusCard.addView(secondaryButton("全屏提醒设置") { openFullScreenIntentSettings() }, lp(top = 10, height = 50))
        statusCard.addView(secondaryButton("勿扰模式访问（可选）") { openDndSettings() }, lp(top = 10, height = 50))
        statusCard.addView(secondaryButton("电池优化设置（可选）") { openBatterySettings() }, lp(top = 10, height = 50))
        root.addView(statusCard, lp(top = 22))

        val plateCard = card()
        plateCard.addView(sectionTitle("🚗 你的爱车车牌"))
        plateInput = EditText(this).apply {
            hint = "一行一个车牌，例如：粤E12345"
            minLines = 2
            maxLines = 5
            textSize = 18f
            setText(Prefs.plates(this@MainActivity).joinToString("\n"))
        }
        plateCard.addView(plateInput, lp(top = 10))
        plateCard.addView(primaryButton("保存车牌") {
            Prefs.savePlates(this, plateInput.text.toString())
            plateInput.setText(Prefs.plates(this).joinToString("\n"))
            toastLike("已保存在本机")
        }, lp(top = 12, height = 52))
        plateCard.addView(note("未保存车牌时会仅按违停关键词识别；保存车牌后误报更少。"), lp(top = 10))
        root.addView(plateCard, lp(top = 18))

        val settingsCard = card()
        settingsCard.addView(sectionTitle("🔔 提醒设置"))

        settingsCard.addView(TextView(this).apply {
            text = "响铃时长"
            textSize = 15f
            setTextColor(0xFF333333.toInt())
        }, lp(top = 8))

        val durations = listOf("30 秒", "60 秒", "120 秒")
        durationSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, durations)
            setSelection(
                when (Prefs.durationSeconds(this@MainActivity)) {
                    30 -> 0
                    120 -> 2
                    else -> 1
                },
            )
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    Prefs.setDurationSeconds(this@MainActivity, listOf(30, 60, 120)[position])
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            }
        }
        settingsCard.addView(durationSpinner, lp(top = 4))

        maxVolumeCheck = CheckBox(this).apply {
            text = "全屏提醒时，临时把闹钟音量调到最大（结束后恢复）"
            textSize = 15f
            isChecked = Prefs.forceMaxVolume(this@MainActivity)
            setOnCheckedChangeListener { _, checked -> Prefs.setForceMaxVolume(this@MainActivity, checked) }
        }
        settingsCard.addView(maxVolumeCheck, lp(top = 8))

        settingsCard.addView(primaryButton("测试强提醒") {
            AlertNotifier.show(
                this,
                AlertPayload(
                    body = "【交警】您的小型汽车粤E12345存在临时停车，请尽快驶离。测试提醒，不会联网。",
                    sender = "10657123123",
                    plate = Prefs.plates(this).firstOrNull() ?: "粤E12345",
                    receivedAtMillis = System.currentTimeMillis(),
                ),
            )
        }, lp(top = 14, height = 52))

        settingsCard.addView(note("网络权限：未声明 INTERNET；车牌与设置只保存在本机 SharedPreferences。"), lp(top = 10))
        root.addView(settingsCard, lp(top = 18))

        root.addView(note("说明：划掉最近任务不会关闭短信广播接收；但系统“强行停止”App 后，Android 会暂停广播投递，需重新打开一次。部分厂商的省电策略也可能影响提醒。"), lp(top = 16))
    }

    private fun refreshStatus() {
        val enabled = Prefs.isEnabled(this)

        val smsPermissionGranted =
            checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val smsObservation = Prefs.lastSmsObservation(this)
        val smsVerified = smsPermissionGranted && smsObservation.hasRealDecodedData

        val manager = getSystemService(NotificationManager::class.java)
        val notificationRuntimePermissionGranted =
            Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val notificationsActuallyEnabled =
            notificationRuntimePermissionGranted && manager.areNotificationsEnabled()

        val fullScreenActuallyAvailable =
            Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent()
        val dndActuallyGranted = manager.isNotificationPolicyAccessGranted

        val power = getSystemService(PowerManager::class.java)
        val batteryActuallyExempt = power.isIgnoringBatteryOptimizations(packageName)

        val requestedPermissions =
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                ?.toSet()
                .orEmpty()
        val networkPermissionActuallyAbsent =
            Manifest.permission.INTERNET !in requestedPermissions &&
                Manifest.permission.ACCESS_NETWORK_STATE !in requestedPermissions

        statusText.text = buildString {
            append(if (enabled) "🔴 功能开关：已开启" else "⚪ 功能开关：已关闭")

            append("\n短信接收验证：")
            when {
                smsVerified -> {
                    append("✓ 已收到并解码真实短信数据")
                    append("\n  最近验证：")
                    append(
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date(smsObservation.observedAtMillis)),
                    )
                    append(" · ")
                    append(smsObservation.partCount)
                    append("段 · ")
                    append(smsObservation.bodyLength)
                    append("字")
                }
                smsPermissionGranted ->
                    append("△ 系统权限已给，但尚未收到真实短信，暂不视为已验证")
                else ->
                    append("✗ 系统未授予 RECEIVE_SMS")
            }

            append("\n通知能力实测：")
            append(if (notificationsActuallyEnabled) "✓ 系统返回可通知" else "✗ 系统返回不可通知")

            append("\n全屏提醒实测：")
            append(
                when {
                    Build.VERSION.SDK_INT < 34 -> "✓ 当前系统无需单独全屏授权"
                    fullScreenActuallyAvailable -> "✓ 系统返回可使用 Full-screen Intent"
                    else -> "✗ 系统返回不可使用 Full-screen Intent"
                },
            )

            append("\n勿扰绕过实测：")
            append(if (dndActuallyGranted) "✓ 系统返回已授权" else "✗ 系统返回未授权（可选）")

            append("\n电池白名单实测：")
            append(if (batteryActuallyExempt) "✓ 系统返回已忽略优化" else "✗ 系统返回未忽略优化（可选）")

            append("\n网络权限实测：")
            append(
                if (networkPermissionActuallyAbsent) {
                    "✓ PackageManager 确认安装包未声明联网权限"
                } else {
                    "✗ 安装包声明了联网相关权限"
                },
            )
        }
        toggleButton.text = if (enabled) "收工" else "开始盯梢"
    }

    private fun requestCorePermissions() {
        val missing = buildList {
            if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.RECEIVE_SMS)
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (missing.isEmpty()) {
            toastLike("必要权限已授予")
        } else {
            requestPermissions(missing.toTypedArray(), 100)
        }
    }

    private fun openFullScreenIntentSettings() {
        if (Build.VERSION.SDK_INT >= 34) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                },
            )
        } else {
            toastLike("当前 Android 版本无需单独授权")
        }
    }

    private fun openDndSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun openBatterySettings() {
        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(Color.WHITE)
        }
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 21f
        setTextColor(0xFF262626.toInt())
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun note(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(0xFF777777.toInt())
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun primaryButton(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 17f
        isAllCaps = false
        setTextColor(Color.WHITE)
        backgroundTintList = ColorStateList.valueOf(0xFFE51C32.toInt())
        setOnClickListener { onClick() }
    }

    private fun secondaryButton(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 15f
        isAllCaps = false
        setTextColor(0xFFD61D31.toInt())
        backgroundTintList = ColorStateList.valueOf(0xFFF7F7F7.toInt())
        setOnClickListener { onClick() }
    }

    private fun toastLike(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        refreshStatus()
    }

    private fun lp(
        top: Int = 0,
        height: Int = LinearLayout.LayoutParams.WRAP_CONTENT,
    ): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            if (height == LinearLayout.LayoutParams.WRAP_CONTENT) height else dp(height),
        ).apply {
            topMargin = dp(top)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
