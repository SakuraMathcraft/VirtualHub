package com.virtual.tensorhub.mock

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import android.app.AppOpsManager
import android.os.Process
import android.os.Binder

class MockLocationProvider(private val context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // 修改为系统 GPS provider
    private val provider = LocationManager.GPS_PROVIDER

    private var started = false

    fun start(): Boolean {
        try {
            // ✅ 使用 AppOpsManager 检查是否具备模拟位置权限
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    context.packageName
                )
            }

            // 如果 mode != AppOpsManager.MODE_ALLOWED，则说明此应用未被选为模拟位置信息应用
            if (mode != AppOpsManager.MODE_ALLOWED) {
                Toast.makeText(context, "请在开发者选项中将本应用设为模拟位置信息应用", Toast.LENGTH_LONG).show()
                try {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MockLocationProvider", "❌ 无法跳转到开发者选项: ${e.message}")
                }
                return false
            }

            // 如果同名 provider 已存在，先移除（防止重复）
            try { locationManager.removeTestProvider(provider) } catch (_: Exception) {}

            locationManager.addTestProvider(
                provider,
                false, false, false, false,
                true, true, true,
                Criteria.ACCURACY_FINE,
                Criteria.POWER_LOW
            )
            locationManager.setTestProviderEnabled(provider, true)
            started = true
            Log.d("MockLocationProvider", "✅ Test provider '$provider' 已启用")
            return true

        } catch (se: SecurityException) {
            Log.e("MockLocationProvider", "❌ 启用 Test provider 失败（权限）: ${se.message}")
            started = false
            return false
        } catch (e: Exception) {
            Log.e("MockLocationProvider", "❌ 启用 Test provider 失败: ${e.message}")
            started = false
            return false
        }
    }

    fun pushLocation(lat: Double, lon: Double, accuracy: Float = 5f): Boolean {
        if (!started) {
            Log.w("MockLocationProvider", "⛔ pushLocation called but provider not started")
            return false
        }

        val location = Location(provider).apply {
            latitude = lat
            longitude = lon
            altitude = 10.0
            this.accuracy = accuracy
            time = System.currentTimeMillis()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }

        return try {
            locationManager.setTestProviderLocation(provider, location)
            Log.d("MockLocationProvider", "📍已注入 ($lat, $lon) via '$provider'")
            true
        } catch (e: Exception) {
            Log.e("MockLocationProvider", "❌ 注入失败: ${e.message}")
            false
        }
    }

    fun stop(): Boolean {
        return try {
            locationManager.removeTestProvider(provider)
            started = false
            Log.d("MockLocationProvider", "🧹 Test provider '$provider' 已移除")
            true
        } catch (e: Exception) {
            Log.w("MockLocationProvider", "移除 provider 失败: ${e.message}")
            false
        }
    }

    fun isStarted(): Boolean = started
}

