package com.virtual.tensorhub.mock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.virtual.tensorhub.MainActivity // 修改为你的主 Activity 路径
import kotlin.math.round

class MockLocationService : Service() {

    companion object {
        const val CHANNEL_ID = "tensorhub_mock_chan"
        const val CHANNEL_NAME = "TensorHub Mock Location"
        const val NOTIF_ID = 0xC0DE
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
        const val ACTION_START = "ACTION_START_MOCK"
        const val ACTION_UPDATE = "ACTION_UPDATE_MOCK"
        const val ACTION_STOP = "ACTION_STOP_MOCK"
        const val UPDATE_INTERVAL_MS = 500L // 每 0.5s 注入一次，防止系统重置
    }

    private lateinit var provider: MockLocationProvider
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var currentLat = 0.0
    private var currentLon = 0.0

    private val tick = object : Runnable {
        override fun run() {
            if (running) {
                provider.pushLocation(currentLat, currentLon)
                updateNotification(currentLat, currentLon)
                handler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        provider = MockLocationProvider(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ✅ 防止系统重启 Service 时传入空 Intent 导致逻辑中断
        if (intent == null) {
            android.util.Log.w("MockLocationService", "Service 重启，intent == null，自动恢复上次状态")
            if (running) handler.post(tick)
            return START_STICKY
        }
        when (intent?.action) {
            ACTION_START -> {
                val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
                val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)
                startMockForeground(lat, lon)
            }
            ACTION_UPDATE -> {
                val lat = intent.getDoubleExtra(EXTRA_LAT, currentLat)
                val lon = intent.getDoubleExtra(EXTRA_LON, currentLon)
                updateCoords(lat, lon)
            }
            ACTION_STOP -> stopMock()
            else -> { /* ignore */ }
        }
        return START_STICKY
    }

    private fun startMockForeground(lat: Double, lon: Double) {
        // ✅ 1. 启动前检查权限，避免在无权限下创建前台服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val fine = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            val fgs = if (Build.VERSION.SDK_INT >= 34)
                checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            else android.content.pm.PackageManager.PERMISSION_GRANTED

            if ((fine != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                        coarse != android.content.pm.PackageManager.PERMISSION_GRANTED)
                || fgs != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                android.util.Log.e("MockLocationService", "缺少前台定位权限，取消启动前台服务")
                stopSelf()
                return
            }
        }

        // ✅ 2. 立即启动前台通知，防止 5 秒超时崩溃
        val notif = buildNotification(lat, lon)
        startForeground(NOTIF_ID, notif)

        // ✅ 3. 启动虚拟定位逻辑
        try {
            if (!running) {
                provider.start()
                currentLat = lat
                currentLon = lon
                running = true
                handler.post(tick)
            } else {
                updateCoords(lat, lon)
            }
        } catch (e: Exception) {
            android.util.Log.e("MockLocationService", "启动 Mock 定位失败: ${e.message}", e)
            stopSelf()
            return
        }

        // ✅ 4. 再次确认权限有效（部分系统可能在启动后动态回收）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val fine = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            val fgs = if (Build.VERSION.SDK_INT >= 34)
                checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            else android.content.pm.PackageManager.PERMISSION_GRANTED

            if ((fine != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                        coarse != android.content.pm.PackageManager.PERMISSION_GRANTED)
                || fgs != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                android.util.Log.e("MockLocationService", "启动后权限被回收，停止服务")
                stopSelf()
            }
        }
    }

    private fun updateCoords(lat: Double, lon: Double) {
        currentLat = lat
        currentLon = lon
        provider.pushLocation(lat, lon)
        updateNotification(lat, lon)
    }

    private fun stopMock() {
        running = false
        handler.removeCallbacksAndMessages(null)
        try { provider.stop() } catch (_: Exception) {}
        stopForeground(true)
        stopSelf()
    }

    private fun buildNotification(lat: Double, lon: Double): Notification {
        val roundedLat = round(lat * 10000) / 10000.0
        val roundedLon = round(lon * 10000) / 10000.0

        val pending = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TensorHub — 虚拟定位已启用")
            .setContentText("注入：$roundedLat , $roundedLon")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(lat: Double, lon: Double) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, buildNotification(lat, lon))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            manager.createNotificationChannel(ch)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        try { provider.stop() } catch (_: Exception) {}
        super.onDestroy()
    }
}
