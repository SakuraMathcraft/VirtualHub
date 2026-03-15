package com.virtual.tensorhub.ui

import android.R.attr.duration
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.virtual.tensorhub.R
import com.amap.api.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

import java.io.File
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import java.util.Locale

// 路径模拟模式
enum class RouteSimMode(val label: String) {
    WALK("步行"),
    BIKE("骑行"),
    CAR("驾车")
}
object AppContextHolder {
    lateinit var context: android.content.Context
    fun init(ctx: android.content.Context) {
        context = ctx.applicationContext
    }
}

/**
 * 路径模拟 UI（仅界面，暂不做真实轨迹逻辑）
 */
@Composable
fun RouteSimulatorScreen(
    navController: NavController
) {
    // ---------- 新增：地图选点状态 ----------
    // 出行模式 & 控制
    var mode by remember { mutableStateOf(RouteSimMode.WALK) }
    var speed by remember { mutableStateOf(3f) }
    var duration by remember { mutableStateOf(10f) }

    val currentSpeed = remember { mutableStateOf(speed) }
    val currentDuration = remember { mutableStateOf(duration) }

    // 控制模式（速度优先 / 时长优先）
    val controlOptions = listOf("以速度为主", "以时长为主")
    var controlMode by remember { mutableStateOf(controlOptions.first()) }

    // 插值算法（全局状态）
    val interpolationMethods = listOf("线性插值", "Bezier 曲线", "Catmull-Rom 样条", "B 样条")
    val handle = navController.currentBackStackEntry?.savedStateHandle
    var selectedMethod by rememberSaveable(handle) {
        mutableStateOf(handle?.get<String>("selected_method") ?: interpolationMethods.first())
    }

    // 从地图返回的原始点 & 派生信息
    val userRoutePoints = remember { mutableStateListOf<LatLng>() }
    var routeSummary by remember { mutableStateOf<String?>(null) }
    var routeDistanceKm by remember { mutableStateOf<Double?>(null) }
    var estimatedText by remember { mutableStateOf("") }

    // 从 MapDrawScreen 接收选点结果
    LaunchedEffect(navController.currentBackStackEntry) {
        val handle = navController.currentBackStackEntry?.savedStateHandle
        val list = handle?.get<List<LatLng>>("route_points")
        if (!list.isNullOrEmpty()) {
            userRoutePoints.clear()
            userRoutePoints.addAll(list)
            handle.remove<List<LatLng>>("route_points")

            routeSummary = "计算中…"
            val (summary, distKm) = withContext(Dispatchers.IO) {
                buildRouteSummary(userRoutePoints.toList(), selectedMethod)
            }
            routeSummary = summary
            routeDistanceKm = distKm
        }
    }

// ✅ 切换插值算法时重算并刷新控制模式预计值
    LaunchedEffect(selectedMethod, userRoutePoints.size) {
        if (userRoutePoints.size >= 2) {
            routeSummary = "计算中…"
            val (summary, distKm) = withContext(Dispatchers.IO) {
                buildRouteSummary(userRoutePoints.toList(), selectedMethod)
            }
            routeSummary = summary
            routeDistanceKm = distKm

            // ✅ 自动刷新控制模式的预计时长/速度
            routeDistanceKm?.let { dist ->
                if (controlMode == "以速度为主") {
                    val totalMinutes = dist * 1000 / (currentSpeed.value / 3.6) / 60
                    estimatedText = "预计时长：${"%.2f".format(totalMinutes)} 分钟"
                } else {
                    val speedAuto = dist * 1000 / (currentDuration.value * 60) * 3.6
                    estimatedText = "对应速度：${"%.2f".format(speedAuto)} km/h"
                }
            }
        }
    }

    // 模拟状态控制
    var isRunning by remember { mutableStateOf(false) }       // 是否正在模拟
    var isPaused by remember { mutableStateOf(false) }        // 是否暂停
    var progress by remember { mutableStateOf(0f) }           // 当前进度 0~100%
    var job by remember { mutableStateOf<Job?>(null) }        // 协程任务句柄

    val context = LocalContext.current

    // 当前选中的预设路线
    var presetRoutes by remember { mutableStateOf(loadPresetRoutes(context)) }

    LaunchedEffect(Unit) {
        presetRoutes = loadPresetRoutes(context) // 确保每次进入页面都刷新
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 IOSButton(
                    onClick = { navController.popBackStack() },
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack, 
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Route Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Walk / Bike / Drive",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Header Animation
             IOSGlassCard {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.Black.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.route_sim))
                    val progressAnim by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = LottieConstants.IterateForever
                    )
                    LottieAnimation(
                        composition = composition,
                        progress = { progressAnim },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }

            // 2. Map & Route Actions
            IOSListGroup(title = "MAP & ROUTE") {
                ListItem(
                    headlineContent = { Text("Draw Route on Map", fontWeight = FontWeight.SemiBold) },
                    supportingContent = { 
                        if (userRoutePoints.size >= 2) {
                            Text("${userRoutePoints.size} points" + (routeDistanceKm?.let { ", ~${"%.2f".format(it)} km" } ?: ""))
                        } else {
                            Text("Tap to open map and select points") 
                        }
                    },
                    leadingContent = { 
                        Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable { navController.navigate("map_draw") }
                )
                
                 if (userRoutePoints.size >= 2) {
                    HorizontalDivider(Modifier.padding(start = 56.dp))
                    ListItem(
                        headlineContent = { Text("Save to Presets", color = MaterialTheme.colorScheme.primary) },
                        leadingContent = { Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            addRouteToPreset(context, userRoutePoints)
                            presetRoutes = loadPresetRoutes(context)
                        }
                    )
                }
            }

            // 3. Mode Selection
            IOSListGroup(title = "MODE") {
                 Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(RouteSimMode.WALK, RouteSimMode.BIKE, RouteSimMode.CAR).forEach { m ->
                         val isSelected = mode == m
                         IOSButton(
                             onClick = { mode = m },
                             modifier = Modifier.weight(1f).height(40.dp),
                             backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                             contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                             shape = RoundedCornerShape(8.dp)
                         ) {
                             Text(m.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                         }
                    }
                }
                HorizontalDivider()
                Text(
                     text = when (mode) {
                        RouteSimMode.WALK -> "Recommended: 3 ~ 6 km/h (Walking)"
                        RouteSimMode.BIKE -> "Recommended: 10 ~ 20 km/h (Cycling)"
                        RouteSimMode.CAR -> "Recommended: 30 ~ 80 km/h (Driving)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
            }

            // 4. Interpolation Method
            IOSListGroup(title = "INTERPOLATION") {
                 Column(Modifier.padding(8.dp)) {
                    interpolationMethods.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { method ->
                                val selected = selectedMethod == method
                                IOSButton(
                                     onClick = {
                                        selectedMethod = method
                                        handle?.set("selected_method", method)
                                     },
                                     modifier = Modifier.weight(1f).height(40.dp).padding(bottom = 8.dp),
                                     backgroundColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                     contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                     shape = RoundedCornerShape(8.dp)
                                ) {
                                    // Modified: allow text to wrap
                                    Text(
                                        text = method, 
                                        fontSize = 10.sp, 
                                        lineHeight = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, 
                                        maxLines = 2,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                 }
            }

            // 5. Speed/Duration Control
            IOSListGroup(title = "CONTROL") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Control Mode", fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .padding(2.dp)
                    ) {
                        controlOptions.forEach { opt ->
                            val active = controlMode == opt
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable { controlMode = opt }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(opt, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
                
                HorizontalDivider(Modifier.padding(start = 16.dp))
                
                Column(Modifier.padding(16.dp)) {
                     if (controlMode == "以速度为主") {
                        Text("Simulated Speed: ${"%.1f".format(speed)} km/h", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Slider(
                            value = speed,
                            onValueChange = {
                                speed = it
                                currentSpeed.value = it
                                if (routeDistanceKm != null) {
                                    val totalMinutes = routeDistanceKm!! * 1000 / (speed / 3.6) / 60
                                    estimatedText = "Est. Time: ${"%.2f".format(totalMinutes)} min"
                                }
                            },
                             valueRange = 1f..30f
                        )
                     } else {
                        Text("Duration: ${"%.1f".format(duration)} min", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Slider(
                             value = duration,
                             onValueChange = {
                                duration = it
                                currentDuration.value = it
                                if (routeDistanceKm != null) {
                                    val speedAuto = routeDistanceKm!! * 1000 / (duration * 60) * 3.6
                                    estimatedText = "Req. Speed: ${"%.2f".format(speedAuto)} km/h"
                                }
                             },
                             valueRange = 1f..60f
                        )
                     }
                     
                     if (estimatedText.isNotEmpty()) {
                        Text(estimatedText, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                     }
                }
            }
            
            // 6. Presets
             IOSListGroup(title = "PRESETS") {
                if (presetRoutes.isEmpty()) {
                    Text("No presets saved.", color = Color.Gray, modifier = Modifier.padding(16.dp), fontSize = 14.sp)
                } else {
                    presetRoutes.forEachIndexed { index, route ->
                        if (index > 0) HorizontalDivider(Modifier.padding(start = 16.dp))
                        
                        val fullName = route["name"] as? String ?: "Route ${index + 1}"
                        val shortName = if ("→" in fullName) {
                           val parts = fullName.split("→").map { it.trim() }
                           if (parts.size >= 2) "${parts.first()} → ${parts.last()}" else fullName
                        } else fullName

                        ListItem(
                            headlineContent = { Text(shortName, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                            supportingContent = { Text("${(route["points"] as? List<*>)?.size ?: 0} points", fontSize = 12.sp, color = Color.Gray) },
                            trailingContent = {
                                IconButton(onClick = {
                                    deletePresetRoute(context, index)
                                    presetRoutes = loadPresetRoutes(context)
                                }) {
                                    Icon(Icons.Rounded.Delete, null, tint = Color.LightGray)
                                }
                            },
                            modifier = Modifier.clickable {
                                val pointsJson = route["points"] as? List<Map<String, Double>> ?: return@clickable
                                val fullPoints = pointsJson.map { LatLng(it["lat"]!!, it["lng"]!!) }
                                userRoutePoints.clear()
                                userRoutePoints.addAll(fullPoints)

                                if (fullPoints.size < 2) {
                                    Toast.makeText(context, "Not enough points", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                Toast.makeText(context, "Route selected. Ready to start.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
             }

            // 7. Running Status
            if (isRunning || progress > 0) {
                 IOSGlassCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isRunning) "Simulating... ${"%.1f".format(progress)}%" else "Finished", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        )
                    }
                 }
            }
            
            // 8. Control Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                 IOSButton(
                    onClick = {
                        if (userRoutePoints.size < 2) {
                            Toast.makeText(context, "Select a route first!", Toast.LENGTH_SHORT).show()
                            return@IOSButton
                        }
                        if (isRunning && !isPaused) return@IOSButton
                        
                        val interpolated = when (selectedMethod) {
                            "Bezier 曲线" -> bezierInterpolation(userRoutePoints)
                            "Catmull-Rom 样条" -> catmullRomInterpolation(userRoutePoints)
                            "B 样条" -> bSplineInterpolation(userRoutePoints)
                            else -> linearInterpolation(userRoutePoints)
                        }
                        val totalDistanceM = pathLength(interpolated)
                        routeDistanceKm = totalDistanceM / 1000.0
                        
                        isRunning = true
                        isPaused = false
                        progress = 0f
                        
                        val totalPoints = interpolated.size

                        job?.cancel()
                        job = CoroutineScope(Dispatchers.IO).launch {
                            val points = interpolated
                            val n = points.size
                             suspend fun uiSetProgress(p: Float) = withContext(Dispatchers.Main) { progress = p }
                            for (i in points.indices) {
                                if (!isRunning) break
                                while (isPaused) delay(200)
                                injectLocation(context, points[i])
                                uiSetProgress(((i + 1) * 100f / n).coerceIn(0f, 100f))
                                
                                val intervalMs = if (controlMode == "以速度为主") {
                                    val s = if (currentSpeed.value < 0.5f) 0.5f else currentSpeed.value
                                    (totalDistanceM / (s / 3.6) * 1000.0 / n)
                                } else {
                                    val d = if (currentDuration.value < 0.5f) 0.5f else currentDuration.value
                                    (d * 60.0 * 1000.0 / n)
                                }
                                delay(intervalMs.toLong().coerceAtLeast(25L))
                            }
                            withContext(Dispatchers.Main) { 
                                isRunning = false 
                                progress = 100f 
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(60.dp),
                    backgroundColor = MaterialTheme.colorScheme.primary 
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start")
                }
                
                if (isRunning) {
                     IOSButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier.weight(1f).height(60.dp),
                        backgroundColor = Color(0xFFFFCC00)
                    ) {
                        Icon(if(isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if(isPaused) "Resume" else "Pause")
                    }
                    
                     IOSButton(
                        onClick = { 
                            isRunning = false
                            isPaused = false
                            job?.cancel()
                        },
                        modifier = Modifier.weight(1f).height(60.dp),
                        backgroundColor = Color(0xFFFF3B30)
                    ) {
                        Icon(Icons.Rounded.Stop, null)
                         Spacer(Modifier.width(8.dp))
                        Text("Stop")
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
// ======================================================
// 🧮 轨迹插值函数定义区（数学算法实现）
// ======================================================

/**
 * 线性插值
 * step：0~1 间的采样步长（例如 0.01 → 每两点间生成约 100 个插值点）
 */
fun linearInterpolation(points: List<LatLng>, step: Double = 0.01): List<LatLng> {
    if (points.size < 2) return points
    val result = mutableListOf<LatLng>()
    for (i in 0 until points.lastIndex) {
        val p1 = points[i]
        val p2 = points[i + 1]
        var t = 0.0
        while (t <= 1.0) {
            val lat = p1.latitude + (p2.latitude - p1.latitude) * t
            val lon = p1.longitude + (p2.longitude - p1.longitude) * t
            result.add(LatLng(lat, lon))
            t += step
        }
    }
    return result
}

/**
 * 二次 Bezier 插值（3 个点为一段）
 * 若点数超过 3，则连续取滑动窗口 [i, i+1, i+2]
 */
fun bezierInterpolation(points: List<LatLng>, step: Double = 0.01): List<LatLng> {
    if (points.size < 3) return linearInterpolation(points, step)
    val result = mutableListOf<LatLng>()
    for (i in 0 until points.size - 2) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val p2 = points[i + 2]
        var t = 0.0
        while (t <= 1.0) {
            val lat = (1 - t).pow(2) * p0.latitude + 2 * (1 - t) * t * p1.latitude + t.pow(2) * p2.latitude
            val lon = (1 - t).pow(2) * p0.longitude + 2 * (1 - t) * t * p1.longitude + t.pow(2) * p2.longitude
            result.add(LatLng(lat, lon))
            t += step
        }
    }
    return result
}

/**
 * Catmull-Rom 样条插值
 * step: 每两控制点间的采样密度
 */
fun catmullRomInterpolation(points: List<LatLng>, step: Double = 0.02): List<LatLng> {
    if (points.size < 4) return linearInterpolation(points, step)
    val result = mutableListOf<LatLng>()
    for (i in 0 until points.size - 3) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val p2 = points[i + 2]
        val p3 = points[i + 3]
        var t = 0.0
        while (t <= 1.0) {
            val t2 = t * t
            val t3 = t2 * t
            val lat = 0.5 * (
                    (2 * p1.latitude) +
                            (-p0.latitude + p2.latitude) * t +
                            (2 * p0.latitude - 5 * p1.latitude + 4 * p2.latitude - p3.latitude) * t2 +
                            (-p0.latitude + 3 * p1.latitude - 3 * p2.latitude + p3.latitude) * t3
                    )
            val lon = 0.5 * (
                    (2 * p1.longitude) +
                            (-p0.longitude + p2.longitude) * t +
                            (2 * p0.longitude - 5 * p1.longitude + 4 * p2.longitude - p3.longitude) * t2 +
                            (-p0.longitude + 3 * p1.longitude - 3 * p2.longitude + p3.longitude) * t3
                    )
            result.add(LatLng(lat, lon))
            t += step
        }
    }
    return result
}

/**
 * B 样条插值（均匀三次 B-spline）
 */
fun bSplineInterpolation(points: List<LatLng>, step: Double = 0.02): List<LatLng> {
    if (points.size < 4) return linearInterpolation(points, step)
    val result = mutableListOf<LatLng>()
    for (i in 0 until points.size - 3) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val p2 = points[i + 2]
        val p3 = points[i + 3]
        var t = 0.0
        while (t <= 1.0) {
            val t2 = t * t
            val t3 = t2 * t
            val lat = (1.0 / 6.0) * (
                    (-t3 + 3 * t2 - 3 * t + 1) * p0.latitude +
                            (3 * t3 - 6 * t2 + 4) * p1.latitude +
                            (-3 * t3 + 3 * t2 + 3 * t + 1) * p2.latitude +
                            (t3) * p3.latitude
                    )
            val lon = (1.0 / 6.0) * (
                    (-t3 + 3 * t2 - 3 * t + 1) * p0.longitude +
                            (3 * t3 - 6 * t2 + 4) * p1.longitude +
                            (-3 * t3 + 3 * t2 + 3 * t + 1) * p2.longitude +
                            (t3) * p3.longitude
                    )
            result.add(LatLng(lat, lon))
            t += step
        }
    }
    return result
}

/**
 * 计算两点间大地距离（米）
 * 使用 Haversine 公式（适用于 WGS84，经纬度 -> 球面距离）
 */
fun distanceOnEarth(p1: LatLng, p2: LatLng): Double {
    val R = 6378137.0 // WGS84 地球半径（单位：米）

    val lat1 = Math.toRadians(p1.latitude)
    val lat2 = Math.toRadians(p2.latitude)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(p2.longitude - p1.longitude)

    val a = sin(dLat / 2).pow(2.0) +
            cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return R * c
}

/**
 * 精确计算整条路径总长度（米）
 */
fun pathLength(points: List<LatLng>): Double {
    if (points.size < 2) return 0.0
    var total = 0.0
    for (i in 0 until points.lastIndex) {
        total += distanceOnEarth(points[i], points[i + 1])
    }
    return total
}
/**
 * 构建路径走向摘要（如：天安门 → 西安 → 厦门）
 */

// 封装：将 reverseGeocode 转为挂起函数
suspend fun reverseGeocodeSync(context: Context, lat: Double, lon: Double): String {
    return suspendCancellableCoroutine { cont ->
        try {
            reverseGeocode(context, lat, lon) { addr ->
                cont.resume(addr)
            }
        } catch (e: Exception) {
            cont.resume("未知地点")
        }
    }
}

suspend fun buildRouteSummary(points: List<LatLng>, method: String): Pair<String, Double?> {
    if (points.size < 2) return "请至少选两个点绘制路径" to null

    val interpolated = when (method) {
        "Bezier 曲线" -> bezierInterpolation(points)
        "Catmull-Rom 样条" -> catmullRomInterpolation(points)
        "B 样条" -> bSplineInterpolation(points)
        else -> linearInterpolation(points)
    }

    val distanceKm = pathLength(interpolated) / 1000.0
    val keyPts = listOf(interpolated.first(), interpolated[interpolated.size / 2], interpolated.last())
    val context = AppContextHolder.context

    val names = mutableListOf<String>()
    for ((i, p) in keyPts.withIndex()) {
        val addr = reverseGeocodeSync(context, p.latitude, p.longitude)
        val name = addr.replace("中国", "")
            .replace("省", "")
            .replace("市", "")
            .replace("区", "")
            .replace("县", "")
            .takeIf { it.isNotBlank() } ?: "地点${i + 1}"
        names.add(name)
    }

    val summary = names.joinToString(" → ")
    return summary to distanceKm
}

fun injectLocation(context: Context, latLng: LatLng) {
    try {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = LocationManager.GPS_PROVIDER

        val location = Location(provider)
        location.latitude = latLng.latitude
        location.longitude = latLng.longitude
        location.accuracy = 3f
        location.time = System.currentTimeMillis()
        location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

        locationManager.setTestProviderLocation(provider, location)
    } catch (e: Exception) {
        Log.e("RouteSim", "注入失败: ${e.message}")
    }
}

fun addRouteToPreset(context: Context, route: List<LatLng>) {
    if (route.isEmpty()) {
        Toast.makeText(context, "当前没有可保存的路线", Toast.LENGTH_SHORT).show()
        return
    }

    val appCtx = context.applicationContext
    val file = File(appCtx.filesDir, "preset_routes.json")
    val gson = Gson()
    val listType = object : TypeToken<MutableList<Map<String, Any>>>() {}.type

    // 初始化文件
    if (!file.exists() || file.length() == 0L) {
        file.createNewFile()
        file.writeText("[]")
    }

    val existing = try {
        gson.fromJson<MutableList<Map<String, Any>>>(file.readText(), listType)
    } catch (e: Exception) {
        mutableListOf<Map<String, Any>>() // 文件损坏时重置
    }

    // 🔄 异步执行：防止主线程阻塞导致闪退
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val (summary, _) = buildRouteSummary(route, "线性插值")
            val newRoute = mapOf(
                "name" to summary,
                "points" to route.map { mapOf("lat" to it.latitude, "lng" to it.longitude) }
            )
            existing.add(newRoute)
            file.writeText(gson.toJson(existing))
            withContext(Dispatchers.Main) {
                Toast.makeText(appCtx, "已保存路线（${route.size} 点）", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(appCtx, "保存失败：${e.message}", Toast.LENGTH_LONG).show()
            }
            Log.e("RouteSim", "addRouteToPreset 异常: ${e.message}", e)
        }
    }
}

fun loadPresetRoutes(context: Context): MutableList<Map<String, Any>> {
    return try {
        val file = File(context.filesDir, "preset_routes.json")
        if (!file.exists()) return mutableListOf()
        val gson = Gson()
        val listType = object : TypeToken<MutableList<Map<String, Any>>>() {}.type
        gson.fromJson(file.readText(), listType) ?: mutableListOf()
    } catch (e: Exception) {
        e.printStackTrace()
        mutableListOf()
    }
}

fun deletePresetRoute(context: Context, index: Int) {
    try {
        val file = File(context.filesDir, "preset_routes.json")
        if (!file.exists()) return
        val gson = Gson()
        val listType = object : TypeToken<MutableList<Map<String, Any>>>() {}.type
        val existing = gson.fromJson<MutableList<Map<String, Any>>>(file.readText(), listType)
        if (index in existing.indices) {
            existing.removeAt(index)
            file.writeText(gson.toJson(existing))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
