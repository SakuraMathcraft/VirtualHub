package com.virtual.tensorhub

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.virtual.tensorhub.ui.*
import com.virtual.tensorhub.ui.loadVirtualLocation

class MainActivity : ComponentActivity() {

    // ✅ 一次性权限申请 Launcher
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val denied = result.filterValues { !it }.keys
            if (denied.isEmpty()) {
                Log.i("MainActivity", "✅ 用户已授予所有权限")
                checkMockAppPermission() // 权限全部通过后再检测“模拟位置信息应用”
                startMockServiceIfNeeded() // 自动恢复虚拟定位服务
            } else {
                Log.w("MainActivity", "⚠️ 用户拒绝了以下权限: $denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 2. 启动 Compose 界面
        setContent {
            val navController = rememberNavController()
            var isDarkMode by rememberSaveable { mutableStateOf(false) }

            TensorHubTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen(
                                navController = navController,
                                isDarkMode = isDarkMode,
                                onToggleTheme = { isDarkMode = !isDarkMode }
                            )
                        }
                        composable("about") { AboutScreen(onBack = { navController.popBackStack() }) }
                        composable("map") { MapScreen(navController = navController) }
                        composable("addressSearch") { AddressSearchScreen(navController = navController) }
                        composable("history") { HistoryScreen(navController = navController) }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // ✅ 确保在 Activity 启动后请求权限（保证 Launcher 已注册）
        requestAllPermissions()
    }

    // ✅ 权限申请逻辑
    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            Log.i("MainActivity", "📋 请求权限: $ungranted")
            permissionLauncher.launch(ungranted.toTypedArray())
        } else {
            Log.i("MainActivity", "✅ 所有权限已授予")
            checkMockAppPermission()
            startMockServiceIfNeeded()
        }
    }

    // ✅ 权限通过后，检查是否设置为模拟位置信息应用
    private fun checkMockAppPermission() {
        val appOps = getSystemService(android.app.AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_MOCK_LOCATION,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_MOCK_LOCATION,
                android.os.Process.myUid(),
                packageName
            )
        }

        if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
            android.app.AlertDialog.Builder(this)
                .setTitle("需要设置模拟位置信息应用")
                .setMessage("请在“开发者选项”中将本应用设为模拟位置信息应用，以启用虚拟定位功能。")
                .setPositiveButton("打开开发者选项") { _, _ ->
                    try {
                        startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                    } catch (_: Exception) {
                        startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
                    }
                }
                .setNegativeButton("稍后") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            Log.i("MainActivity", "✅ 已设置为模拟位置信息应用")
        }
    }

    // ✅ 启动 MockLocationService（权限全部通过后调用）
    private fun startMockServiceIfNeeded() {
        val savedLoc = loadVirtualLocation(this)
        if (savedLoc == null) {
            Log.w("MainActivity", "⚠️ 未找到保存的虚拟位置，不启动服务")
            return
        }

        try {
            val intent = android.content.Intent(
                this,
                com.virtual.tensorhub.mock.MockLocationService::class.java
            ).apply {
                action = com.virtual.tensorhub.mock.MockLocationService.ACTION_START
                putExtra(com.virtual.tensorhub.mock.MockLocationService.EXTRA_LAT, savedLoc.latitude)
                putExtra(com.virtual.tensorhub.mock.MockLocationService.EXTRA_LON, savedLoc.longitude)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(intent)
            else
                startService(intent)

            Log.i("MainActivity", "✅ 已恢复虚拟定位服务并注入坐标 (${savedLoc.latitude}, ${savedLoc.longitude})")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ 启动前台服务失败: ${e.message}")
        }
    }
}