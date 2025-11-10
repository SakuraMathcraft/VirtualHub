package com.virtual.tensorhub.ui

import android.util.Log
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.livedata.observeAsState
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.launch
import com.virtual.tensorhub.mock.MockLocationProvider
import com.amap.api.maps.model.LatLng
import com.virtual.tensorhub.ui.loadVirtualLocation
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.GeocodeQuery
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.amap.api.services.core.LatLonPoint

@Composable
fun TopSection(isSuccess: Boolean) {
    // ✅ 根据状态切换动画文件
    val animationFile = if (isSuccess) "success.json" else "loading.json"
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(animationFile))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        // loading 循环播放，success 播放一次：简单点这里先都循环，后面可以分开控制
        iterations = if (isSuccess) 1 else LottieConstants.IterateForever
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(140.dp)
        )
        Text(
            text = "TensorHub",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Text(
            text = if (isSuccess) "succeed" else "准备中",
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
fun MainScreen(
    navController: NavController,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit
){
    val scrollState = rememberScrollState()

    // ✅ 控制虚拟定位状态
    var isMockRunning by rememberSaveable { mutableStateOf(false) }

    // ✅ 优先读取上次保存的虚拟坐标
    val context = LocalContext.current
    val savedVirtualPos = loadVirtualLocation(context)
    var currentLocation by rememberSaveable {
        mutableStateOf(
            if (savedVirtualPos != null)
                "自定义位置 (${String.format("%.4f", savedVirtualPos.latitude)}, ${String.format("%.4f", savedVirtualPos.longitude)})"
            else "思学楼 A 区"
        )
    }

    var currentLatLng by rememberSaveable { mutableStateOf("未定位") }

    // ✅ 彩带动画显示控制
    var showConfetti by remember { mutableStateOf(false) }

    // ✅ 改进版：先取 LiveData，再安全 observe
    val pickedLocationLiveData = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<com.amap.api.maps.model.LatLng>("pickedLocation")

    val pickedLocationState = pickedLocationLiveData?.observeAsState()
    val pickedLocation = pickedLocationState?.value

    LaunchedEffect(Unit) {
        val last = loadVirtualLocation(context)
        if (last != null) {
            val mock = com.virtual.tensorhub.mock.MockLocationProvider(context)
            mock.start()
            mock.pushLocation(last.latitude, last.longitude)
            isMockRunning = true

            reverseGeocode(context, last.latitude, last.longitude) { addr ->
                currentLocation = addr
                currentLatLng = String.format("%.4f° N, %.4f° E", last.latitude, last.longitude)
            }

            Log.d("MainScreen", "📍恢复虚拟位置: ${last.latitude}, ${last.longitude}")
        }
    }

    // ✅ 防止多次注入虚拟位置
    LaunchedEffect(pickedLocation) {
        pickedLocation?.let { latLng ->
            val mock = com.virtual.tensorhub.mock.MockLocationProvider(context)
            mock.start()
            mock.pushLocation(latLng.latitude, latLng.longitude)

            reverseGeocode(context, latLng.latitude, latLng.longitude) { addr ->
                currentLocation = addr
                currentLatLng = String.format("%.4f° N, %.4f° E", latLng.latitude, latLng.longitude)
            }

            isMockRunning = true

            android.widget.Toast.makeText(
                context,
                "虚拟位置已注入：${latLng.latitude}, ${latLng.longitude}",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            // ✅ 仅执行一次后移除
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<com.amap.api.maps.model.LatLng>("pickedLocation")
        }
    }

    // ✅ 当虚拟定位成功(isMockRunning = true)时触发庆祝动画
    LaunchedEffect(isMockRunning) {
        if (isMockRunning) {
            showConfetti = true
            delay(3200)      // 彩带显示 3.2 秒，可自己调
            showConfetti = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDarkMode)
                        listOf(Color(0xFF0F1724), Color(0xFF1A1A1A)) // 暗色模式背景
                    else
                        listOf(Color(0xFF6A11CB), Color(0xFF2575FC)) // 亮色模式背景
                )
            )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部动画区：根据 isMockRunning 决定 loading / succeed
            TopSection(isSuccess = isMockRunning)

            // 📍 当前定位信息卡
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "当前位置",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = currentLocation,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = currentLatLng,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // ✅ 右侧虚拟定位状态提示
                    if (isMockRunning) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "虚拟位置",
                                color = Color(0xFFB2FF59),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // ⚙️ 功能操作区（带浮动玻璃背景）
            GlassCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "功能操作",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleMedium
                    )

                    // 按钮组
                    FunctionSection(
                        onMapClick = {
                            navController.navigate("map")   // ✅ 跳转到 MapScreen
                        },
                        onGetRealLocation = {
                            isMockRunning = false
                            currentLocation = "真实定位"
                        },
                        onStopMock = {
                            val intent = android.content.Intent(context, com.virtual.tensorhub.mock.MockLocationService::class.java).apply {
                                action = com.virtual.tensorhub.mock.MockLocationService.ACTION_STOP
                            }
                            context.startService(intent)

                            isMockRunning = false
                            currentLocation = "已停止虚拟定位"
                            currentLatLng = "这里将显示经纬度"
                            android.widget.Toast.makeText(context, "已停止虚拟定位", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onSearchClick = { navController.navigate("addressSearch") },
                        onCollectClick = { navController.navigate("history") },
                        onRouteClick = { /* 路径模拟 */ },
                        onMockStarted = { isMockRunning = true }
                    )
                }
            }

            // 📶 快速选择区域（带 Popover 下拉菜单）
            GlassCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "快速选择",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "点击楼名卡片可选择具体区域（A/B/C区）",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    // ✅ 各教学楼与分区的坐标映射表（示例坐标，请换成你自己的）
                    val buildingCoords = mapOf(
                        "思学楼" to mapOf(
                            "A区" to LatLng(30.8214, 104.1850),
                            "B区" to LatLng(30.8210, 104.1848),
                            "C区" to LatLng(30.8205, 104.1848)
                        ),
                        "博学楼" to mapOf(
                            "A区" to LatLng(30.8226, 104.1860),
                            "B区" to LatLng(30.8222, 104.1865),
                            "C区" to LatLng(30.8224, 104.1866)
                        ),
                        "明理楼" to mapOf(
                            "A区" to LatLng(30.8302, 104.1840),
                            "B区" to LatLng(30.8304, 104.1838),
                            "C区" to LatLng(30.8305, 104.1841)
                        ),
                        "明德楼" to mapOf(
                            "A区" to LatLng(30.8318, 104.1840),
                            "B区" to LatLng(30.8315, 104.1842),
                            "C区" to LatLng(30.8314, 104.1841)
                        )
                    )

                    val buildings = listOf("思学楼", "博学楼", "明理楼", "明德楼")
                    var expandedBuilding by remember { mutableStateOf<String?>(null) }
                    var selectedBuilding by remember { mutableStateOf<String?>(null) } // ✅ 新增：记录已选择教学楼
                    val subAreas = listOf("A区", "B区", "C区")

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 快速选择区域部分
                        buildings.chunked(2).forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                row.forEach { building ->
                                    val isSelected = selectedBuilding == building
                                    val isExpanded = expandedBuilding == building
                                    val isActive = isSelected || isExpanded  // ✅ 高亮条件：已选 或 正在展开

                                    // 3D 浮动 + 柔光
                                    val transition =
                                        updateTransition(isActive, label = "cardTransition")
                                    val scale by transition.animateFloat(
                                        label = "scale",
                                        transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) }
                                    ) { if (it) 1.06f else 1f }

                                    val shadow by transition.animateDp(
                                        label = "shadow",
                                        transitionSpec = {
                                            tween(
                                                durationMillis = 300,
                                                easing = FastOutSlowInEasing
                                            )
                                        }
                                    ) { if (it) 14.dp else 0.dp }

                                    val alpha by transition.animateFloat(
                                        label = "alpha",
                                        transitionSpec = { tween(300) }
                                    ) { if (it) 0.6f else 0.2f }

                                    // ✅ 选中时播放 Lottie 动画
                                    val lottieRes =
                                        if (isActive) "selected.json" else "pin_idle.json"
                                    val composition by rememberLottieComposition(
                                        LottieCompositionSpec.Asset(lottieRes)
                                    )
                                    val progress by animateLottieCompositionAsState(
                                        composition = composition,
                                        iterations = LottieConstants.IterateForever,
                                        restartOnPlay = false
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(90.dp)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .shadow(
                                                elevation = shadow,
                                                shape = RoundedCornerShape(20.dp),
                                                ambientColor = Color.White.copy(alpha = 0.4f),
                                                spotColor = Color.White.copy(alpha = 0.45f)
                                            )
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = alpha),
                                                        Color.White.copy(alpha = alpha * 0.8f)
                                                    )
                                                )
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = if (isActive) 0.6f else 0.2f),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .clickable {
                                                expandedBuilding =
                                                    if (isExpanded) null else building
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            LottieAnimation(
                                                composition = composition,
                                                progress = { progress },
                                                modifier = Modifier
                                                    .size(if (isActive) 104.dp else 44.dp) // 非选中大约 44dp，选中放大到 104dp
                                            )
                                            Text(
                                                text = building,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "A-C",
                                                color = Color.White.copy(alpha = 0.7f),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ✅ Popover 展开 + 回收动效（更顺滑 + 明显下拉）
                        AnimatedVisibility(
                            visible = expandedBuilding != null,
                            enter = fadeIn(animationSpec = tween(250)) + expandVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = 260f
                                )
                            ),
                            exit = fadeOut(animationSpec = tween(250)) + shrinkVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = 180f
                                )
                            )
                        ) {
                            expandedBuilding?.let { building ->
                                val context = LocalContext.current
                                val vibrator = remember {
                                    ContextCompat.getSystemService(context, Vibrator::class.java)
                                }

                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "选择区域 - $building",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleSmall
                                        )

                                        subAreas.forEach { area ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(
                                                                Color.White.copy(alpha = 0.12f),
                                                                Color.White.copy(alpha = 0.05f)
                                                            )
                                                        )
                                                    )
                                                    .border(
                                                        1.dp,
                                                        Color.White.copy(alpha = 0.18f),
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable {
                                                        // 触觉反馈（带权限/异常保护你已经配好）
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            try {
                                                                vibrator?.vibrate(
                                                                    VibrationEffect.createOneShot(
                                                                        60L,
                                                                        VibrationEffect.DEFAULT_AMPLITUDE
                                                                    )
                                                                )
                                                            } catch (_: SecurityException) {
                                                            }
                                                        } else {
                                                            @Suppress("DEPRECATION")
                                                            try {
                                                                vibrator?.vibrate(60L)
                                                            } catch (_: SecurityException) {
                                                            }
                                                        }

                                                        // ✅ 读取坐标
                                                        val coord = buildingCoords[building]?.get(area)
                                                        if (coord != null) {
                                                            // ✅ 启动 MockLocationService（带前台通知 & 持续注入）
                                                            val intent = android.content.Intent(
                                                                context,
                                                                com.virtual.tensorhub.mock.MockLocationService::class.java
                                                            ).apply {
                                                                action = com.virtual.tensorhub.mock.MockLocationService.ACTION_START
                                                                putExtra(com.virtual.tensorhub.mock.MockLocationService.EXTRA_LAT, coord.latitude)
                                                                putExtra(com.virtual.tensorhub.mock.MockLocationService.EXTRA_LON, coord.longitude)
                                                            }
                                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                                                                context.startForegroundService(intent)
                                                            else
                                                                context.startService(intent)

                                                            // ✅ 保存虚拟坐标
                                                            saveVirtualLocation(context, coord.latitude, coord.longitude)

                                                            // ✅ 反向地理编码更新 UI
                                                            reverseGeocode(context, coord.latitude, coord.longitude) { addr ->
                                                                currentLocation = addr
                                                                currentLatLng = String.format("%.4f° N, %.4f° E", coord.latitude, coord.longitude)
                                                            }

                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "虚拟位置已切换到：$building $area",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        } else {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "未配置 $building $area 的坐标！",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        }

                                                        // ✅ UI 状态同步更新
                                                        isMockRunning = true
                                                        selectedBuilding = building
                                                        expandedBuilding = null
                                                    }
                                                    .padding(12.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color.White.copy(alpha = 0.3f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = area.first().toString(),
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelMedium
                                                        )
                                                    }
                                                    Column {
                                                        Text(
                                                            text = area,
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        val coord = buildingCoords[building]?.get(area)
                                                        Text(
                                                            text = if (coord != null)
                                                                String.format("%.4f° N, %.4f° E", coord.latitude, coord.longitude)
                                                            else
                                                                "未配置坐标",
                                                            color = Color.White.copy(alpha = 0.7f),
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 💡 使用提示
            GlassCard {
                Column {
                    Text(
                        text = "使用提示",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "· SWPU用户可以点击快速选择的特定教学楼区\n" +
                                "· 使用地图选点可精准选择特定虚拟位置\n" +
                                "· 设置为开发者选项中的模拟位置信息应用后生效",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // 🎉 彩带庆祝动画覆盖层（放在 Column 之后，始终在顶层）
        AnimatedVisibility(
            visible = showConfetti,
            enter = fadeIn(animationSpec = tween(260)),
            exit = fadeOut(animationSpec = tween(420))
        ) {
            val confettiComposition by rememberLottieComposition(
                LottieCompositionSpec.Asset("confetti.json")
            )
            val confettiProgress by animateLottieCompositionAsState(
                composition = confettiComposition,
                iterations = 1
            )

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter   // 顶部洒落感；想居中就改 Center
            ) {
                LottieAnimation(
                    composition = confettiComposition,
                    progress = { confettiProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)   // 只占上方区域，避免挡住全部 UI
                )
            }
        }
        // 🌙 左右上角悬浮按钮层
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarPadding + 12.dp,  // 状态栏高度 + 额外 12dp 间距
                    bottom = 16.dp
                )
        ) {
            // 左上角主题切换
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(46.dp),
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50),
                tonalElevation = 4.dp,
                shadowElevation = 6.dp
            ) {
                IconButton(onClick = onToggleTheme) {      // ✅ 调用外部传入函数
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.Nightlight,
                        contentDescription = "切换模式",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 右上角信息按钮
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(46.dp),
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50),
                tonalElevation = 4.dp,
                shadowElevation = 6.dp
            ) {
                IconButton(onClick = { navController.navigate("about") }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "应用信息",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun Modifier.bounceClick(onClick: () -> Unit): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 500f
        ),
        label = "bounceClickScale"
    )

    val coroutineScope = rememberCoroutineScope()

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable {
            coroutineScope.launch {
                pressed = true
                delay(70)
                pressed = false
                onClick()
            }
        }
}
fun reverseGeocode(
    context: android.content.Context,
    lat: Double,
    lng: Double,
    onAddressFound: (String) -> Unit
) {
    val geocoderSearch = GeocodeSearch(context)
    geocoderSearch.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
        override fun onRegeocodeSearched(result: RegeocodeResult?, rCode: Int) {
            if (rCode == 1000 && result != null) {
                val address = result.regeocodeAddress?.formatAddress ?: "未知位置"
                onAddressFound(address)
            } else {
                onAddressFound("未知位置")
            }
        }
        override fun onGeocodeSearched(result: com.amap.api.services.geocoder.GeocodeResult?, rCode: Int) {}
    })
    val query = RegeocodeQuery(LatLonPoint(lat, lng), 200f, GeocodeSearch.AMAP)
    geocoderSearch.getFromLocationAsyn(query)
}
