package com.virtual.tensorhub.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.virtual.tensorhub.mock.MockLocationService

private const val PREFS_NAME = "virtual_location"
private const val KEY_LAT = "saved_lat"
private const val KEY_LNG = "saved_lng"

fun saveVirtualLocation(context: android.content.Context, lat: Double, lng: Double) {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    prefs.edit().putFloat(KEY_LAT, lat.toFloat()).putFloat(KEY_LNG, lng.toFloat()).apply()
}

fun loadVirtualLocation(context: android.content.Context): LatLng? {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val lat = prefs.getFloat(KEY_LAT, Float.NaN)
    val lng = prefs.getFloat(KEY_LNG, Float.NaN)
    return if (!lat.isNaN() && !lng.isNaN()) LatLng(lat.toDouble(), lng.toDouble()) else null
}

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var aMap by remember { mutableStateOf<AMap?>(null) }

    // ✅ 生命周期绑定
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ✅ 权限申请器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (!granted) {
            Toast.makeText(context, "请授予定位权限以使用当前位置功能", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ 弹窗状态
    var showDialog by remember { mutableStateOf(false) }

    // ✅ 检查权限与定位服务状态
    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            coarse != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            val locationManager = context.getSystemService(android.location.LocationManager::class.java)
            val gps = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
            val net = locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
            if (!gps && !net) showDialog = true
        }
    }

    // ✅ Material3 风格提示框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("定位服务未开启") },
            text = { Text("请开启定位服务以启用地图定位功能。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                ) { Text("前往开启") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消") }
            }
        )
    }

    // ✅ 初始化地图
    LaunchedEffect(Unit) {
        aMap = mapView.map
        aMap?.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isMyLocationButtonEnabled = true

            val myLocationStyle = com.amap.api.maps.model.MyLocationStyle().apply {
                interval(2000)
                myLocationType(com.amap.api.maps.model.MyLocationStyle.LOCATION_TYPE_FOLLOW)
                showMyLocation(true)
            }
            myLocationStyle.strokeColor(android.graphics.Color.TRANSPARENT)
            myLocationStyle.radiusFillColor(0x220000FF)
            setMyLocationStyle(myLocationStyle)
            isMyLocationEnabled = true

            // ✅ 优先使用保存的虚拟位置
            val savedLocation = loadVirtualLocation(context)
            if (savedLocation != null) {
                moveCamera(CameraUpdateFactory.newLatLngZoom(savedLocation, 16f))
                addMarker(MarkerOptions().position(savedLocation).title("上次选定位置"))
                Log.d("MapScreen", "📍恢复上次虚拟位置: ${savedLocation.latitude}, ${savedLocation.longitude}")
            } else {
                // ✅ 没有保存 → 执行一次定位
                val locationClient = com.amap.api.location.AMapLocationClient(context)
                val option = com.amap.api.location.AMapLocationClientOption().apply {
                    locationMode = com.amap.api.location.AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                    isOnceLocation = true
                    isNeedAddress = true
                }
                locationClient.setLocationOption(option)
                locationClient.setLocationListener { loc ->
                    if (loc != null && loc.errorCode == 0) {
                        val pos = LatLng(loc.latitude, loc.longitude)
                        moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                        Log.d("MapScreen", "📍首次定位成功: ${loc.latitude}, ${loc.longitude}")
                    } else {
                        Log.e("MapScreen", "定位失败: ${loc.errorInfo}")
                    }
                }
                locationClient.startLocation()
            }

            // ✅ 点击地图选点
            setOnMapClickListener { latLng ->
                clear()
                addMarker(MarkerOptions().position(latLng).title("选中点"))
                saveVirtualLocation(context, latLng.latitude, latLng.longitude)

                // 启动 MockLocationService 前台注入
                val intent = Intent(context, MockLocationService::class.java).apply {
                    action = com.virtual.tensorhub.mock.MockLocationService.ACTION_START
                    putExtra(com.virtual.tensorhub.mock.MockLocationService.EXTRA_LAT, latLng.latitude)
                    putExtra(com.virtual.tensorhub.mock.MockLocationService.EXTRA_LON, latLng.longitude)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    context.startForegroundService(intent)
                else
                    context.startService(intent)

                Toast.makeText(context, "虚拟定位已切换", Toast.LENGTH_SHORT).show()

                Toast.makeText(context, "选中：${latLng.latitude}, ${latLng.longitude}", Toast.LENGTH_SHORT).show()
                navController.previousBackStackEntry?.savedStateHandle?.set("pickedLocation", latLng)
                mapView.postDelayed({ navController.popBackStack() }, 180)
            }
        }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
    BackHandler { navController.popBackStack() }
}
