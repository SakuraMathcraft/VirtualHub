package com.virtual.tensorhub.ui

import android.content.Intent
import android.util.Log
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale // Added
import androidx.compose.ui.draw.alpha // Added
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.virtual.tensorhub.mock.MockLocationService

@Composable
fun TopSection(isSuccess: Boolean) {
    val animationFile = if (isSuccess) "success.json" else "loading.json"
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(animationFile))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (isSuccess) 1 else LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(16.dp, CircleShape, ambientColor = Color.Blue.copy(alpha = 0.2f), spotColor = Color.Blue.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(Color.White)
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.fillMaxSize().padding(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "TensorHub",
            style = MaterialTheme.typography.headlineLarge, // Uses our new Apple Hierarchy
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (isSuccess) "Virtual Location Active" else "Ready to simulate",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
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

        // ✅ 实际检测 MockLocationService 是否正在运行
        val isServiceRunning = try {
            val activityManager = context.getSystemService(android.app.ActivityManager::class.java)
            @Suppress("DEPRECATION")
            activityManager?.getRunningServices(Int.MAX_VALUE)
                ?.any { it.service.className == "com.virtual.tensorhub.mock.MockLocationService" } == true
        } catch (e: Exception) {
            false
        }

        if (last != null && isServiceRunning) {
            val mock = com.virtual.tensorhub.mock.MockLocationProvider(context)
            mock.start()
            mock.pushLocation(last.latitude, last.longitude)
            isMockRunning = true

            reverseGeocode(context, last.latitude, last.longitude) { addr ->
                currentLocation = addr
                currentLatLng = String.format("%.4f° N, %.4f° E", last.latitude, last.longitude)
            }

            Log.d("MainScreen", "📍恢复虚拟位置: ${last.latitude}, ${last.longitude}")
        } else {
            isMockRunning = false
            Log.d("MainScreen", "未检测到虚拟定位服务，保持准备中状态")
        }
    }

    // ✅ 防止多次注入虚拟位置
    LaunchedEffect(pickedLocation) {
        pickedLocation?.let { latLng ->
            try {
                // ✅ 尝试启动 MockLocationService
                val mock = com.virtual.tensorhub.mock.MockLocationProvider(context)
                mock.start()
                mock.pushLocation(latLng.latitude, latLng.longitude)

                // ✅ 如果执行成功，更新状态
                isMockRunning = true

                reverseGeocode(context, latLng.latitude, latLng.longitude) { addr ->
                    currentLocation = addr
                    currentLatLng = String.format(
                        "%.4f° N, %.4f° E",
                        latLng.latitude,
                        latLng.longitude
                    )
                }

                Toast.makeText(
                    context,
                    "虚拟位置已注入：${latLng.latitude}, ${latLng.longitude}",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                // ❌ 出现异常（未设模拟应用 / 注入失败等）
                Log.e("MainScreen", "虚拟定位注入失败: ${e.message}")
                isMockRunning = false
                Toast.makeText(
                    context,
                    "虚拟定位未成功，请检查是否已设为模拟位置信息应用",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // ✅ 无论成功或失败都清除一次性状态
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<com.amap.api.maps.model.LatLng>("pickedLocation")
            }
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                horizontalArrangement = Arrangement.SpaceBetween, // Important: SpaceBetween pushes items to edges
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme Toggle with Radiating Ripple Effect
                Box(contentAlignment = Alignment.Center) {
                    val scaleAnim = remember { Animatable(0f) }
                    val alphaAnim = remember { Animatable(0.6f) }
                    val scope = rememberCoroutineScope()
                    
                    if (scaleAnim.value > 0f) {
                         Box(
                            modifier = Modifier
                                .size(44.dp)
                                .scale(scaleAnim.value)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            if (!isDarkMode) Color(0xFFFFCC00).copy(alpha=0.8f) else Color.White.copy(alpha=0.3f), // Inverse logic for contrast
                                            Color.Transparent
                                        )
                                    )
                                )
                                .alpha(alphaAnim.value)
                        )
                    }

                    IOSButton(
                        onClick = { 
                            scope.launch {
                                // Reset
                                scaleAnim.snapTo(0f)
                                alphaAnim.snapTo(0.6f)
                                
                                // Animate
                                launch {
                                    scaleAnim.animateTo(
                                        targetValue = 20f, // Large bloom covering screen
                                        animationSpec = tween(1000, easing = FastOutSlowInEasing)
                                    )
                                }
                                launch {
                                    alphaAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(1000)
                                    )
                                }
                            }
                            onToggleTheme() 
                        },
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.Nightlight,
                            contentDescription = "Theme",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Info
                IOSButton(
                    onClick = { navController.navigate("about") },
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // 1. Header Section
                item {
                    TopSection(isSuccess = isMockRunning)
                }

                // 2. Status Card
                item {
                    IOSListGroup(title = "Location Status") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = currentLocation,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentLatLng,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            
                            if (isMockRunning) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF34C759)) // iOS Green
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "VIRTUAL",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Actions Grid
                item {
                    Text(
                        text = "ACTIONS",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Row 1: Main Actions
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Map Select
                            IOSButton(
                                onClick = { navController.navigate("map") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp),
                                backgroundColor = MaterialTheme.colorScheme.primary
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Map, null, modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Start Map", fontWeight = FontWeight.Medium)
                                }
                            }

                            // Stop Mock
                            IOSButton(
                                onClick = {
                                    try {
                                        val activityManager = context.getSystemService(android.app.ActivityManager::class.java)
                                        val isRunning = try {
                                            @Suppress("DEPRECATION")
                                            activityManager?.getRunningServices(Int.MAX_VALUE)
                                                ?.any { it.service.className == "com.virtual.tensorhub.mock.MockLocationService" } == true
                                        } catch (e: Exception) {
                                            false
                                        }

                                        if (isRunning) {
                                            val intent = Intent(context, com.virtual.tensorhub.mock.MockLocationService::class.java).apply {
                                                action = com.virtual.tensorhub.mock.MockLocationService.ACTION_STOP
                                            }

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                                context.startForegroundService(intent)
                                            else
                                                context.startService(intent)

                                            isMockRunning = false
                                            currentLocation = "Stopped"
                                            currentLatLng = "Real Location Pending..."
                                            Toast.makeText(context, "Virtual location stopped", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Already stopped", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainScreen", "Stop failed: ${e.message}")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp),
                                backgroundColor = Color(0xFFFF3B30) // iOS Red
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Stop", fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // Row 2: Secondary Actions
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val secondaryBtnColor = MaterialTheme.colorScheme.surface
                            val secondaryContentColor = MaterialTheme.colorScheme.onSurface
                            
                            IOSButton(onClick = { navController.navigate("addressSearch") }, modifier = Modifier.weight(1f).height(60.dp), backgroundColor = secondaryBtnColor, contentColor = secondaryContentColor) {
                                Icon(Icons.Default.Search, null)
                            }
                            IOSButton(onClick = { navController.navigate("history") }, modifier = Modifier.weight(1f).height(60.dp), backgroundColor = secondaryBtnColor, contentColor = secondaryContentColor) {
                                Icon(Icons.Default.History, null)
                            }
                             IOSButton(onClick = { navController.navigate("route_simulator") }, modifier = Modifier.weight(1f).height(60.dp), backgroundColor = secondaryBtnColor, contentColor = secondaryContentColor) {
                                Icon(Icons.Default.AltRoute, null)
                            }
                        }
                    }
                }

                // 4. Building Picker (Legacy Logic Adapted)
                item {
                     // ✅ 各教学楼与分区的坐标映射表
                    val buildingCoords = mapOf(
                        "思学楼" to mapOf(
                            "A区" to LatLng(30.8214, 104.1850),
                            "B区" to LatLng(30.8210, 104.1849),
                            "C区" to LatLng(30.8205, 104.1847)
                        ),
                        "博学楼" to mapOf(
                            "A区" to LatLng(30.8229, 104.1863),
                            "B区" to LatLng(30.8224, 104.1862),
                            "C区" to LatLng(30.8223, 104.1864)
                        ),
                        "明理楼" to mapOf(
                            "A区" to LatLng(30.8299, 104.1839),
                            "B区" to LatLng(30.8317, 104.1855),
                            "C区" to LatLng(30.8293, 104.1839)
                        ),
                        "明德楼" to mapOf(
                            "A区" to LatLng(30.8318, 104.1840),
                            "B区" to LatLng(30.8318, 104.1840),
                            "C区" to LatLng(30.8318, 104.1840)
                        )
                    )
                    
                    val buildings = listOf("思学楼", "博学楼", "明理楼", "明德楼")
                    
                    Text(
                        text = "QUICK LOCATIONS",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        buildings.chunked(2).forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                row.forEach { building ->
                                    // State for dropdown menu
                                    var expanded by remember { mutableStateOf(false) }

                                    Box(modifier = Modifier.weight(1f)) {
                                        IOSButton(
                                            onClick = { expanded = true },
                                            modifier = Modifier.fillMaxWidth().height(80.dp),
                                            backgroundColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(building, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text("Tap to select area", fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            buildingCoords[building]?.forEach { (area, coord) ->
                                                DropdownMenuItem(
                                                    text = { 
                                                        Text(
                                                            "$building $area", 
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        ) 
                                                    },
                                                    onClick = {
                                                        expanded = false
                                                        try {
                                                            val intent = Intent(context, com.virtual.tensorhub.mock.MockLocationService::class.java).apply {
                                                                action = com.virtual.tensorhub.mock.MockLocationService.ACTION_START
                                                                putExtra(com.virtual.tensorhub.mock.MockLocationService.EXTRA_LAT, coord.latitude)
                                                                putExtra(com.virtual.tensorhub.mock.MockLocationService.EXTRA_LON, coord.longitude)
                                                            }
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                                                context.startForegroundService(intent)
                                                            else
                                                                context.startService(intent)

                                                            saveVirtualLocation(context, coord.latitude, coord.longitude)
                                                            isMockRunning = true
                                                            currentLocation = "$building $area"
                                                            currentLatLng = String.format("%.4f, %.4f", coord.latitude, coord.longitude)
                                                            Toast.makeText(context, "Switched to $building $area", Toast.LENGTH_SHORT).show()
                                                        } catch (e: Exception) {
                                                            Log.e("MainScreen", "Building switch failed", e)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Note: The previous logic for building selection was complex with expanded states.
                    // For modern UI, maybe just a list of "Favorites" or "Nearby"?
                    // I realized I simplified the building selector too much and broke the interaction.
                    // Let's bring back a cleaner version of the expansion in a separate component if needed.
                    // Or actually, let's use the IOSListGroup for buildings!
                }
                
                // 5. Hint
                 item {
                     Text(
                        text = "Develop Mode Required. Ensure this app is selected as Mock Location App in Android Developer Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                    )
                 }
            }

            // Confetti Overlay
             AnimatedVisibility(
                visible = showConfetti,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val confettiComposition by rememberLottieComposition(LottieCompositionSpec.Asset("confetti.json"))
                val confettiProgress by animateLottieCompositionAsState(composition = confettiComposition)
                
                LottieAnimation(
                    composition = confettiComposition,
                    progress = { confettiProgress },
                    modifier = Modifier.fillMaxSize().padding(top = 50.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                )
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
