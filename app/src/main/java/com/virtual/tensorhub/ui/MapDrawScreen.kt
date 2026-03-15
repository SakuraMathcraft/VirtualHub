package com.virtual.tensorhub.ui

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.help.Tip
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDrawScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapViewBundle = rememberSaveable { Bundle() } // ✅ 用于保存地图状态

    val mapView = remember { MapView(context) }
    val routePoints = remember { mutableStateListOf<LatLng>() }
    var aMap by remember { mutableStateOf<com.amap.api.maps.AMap?>(null) }

    // 绑定生命周期
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_CREATE -> mapView.onCreate(mapViewBundle)
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 初始化地图
    LaunchedEffect(Unit) {
        aMap = mapView.map
        aMap?.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isCompassEnabled = false
            uiSettings.isScaleControlsEnabled = true
            moveCamera(com.amap.api.maps.CameraUpdateFactory.zoomTo(17f))

            // 点击加点 + 画线
            setOnMapClickListener { latLng ->
                routePoints.add(latLng)
                clear()
                addPolyline(
                    PolylineOptions()
                        .addAll(routePoints)
                        .color(0xFF00E5FF.toInt())
                        .width(8f)
                        .useGradient(true)
                )
                addMarker(MarkerOptions().position(latLng).title("点 ${routePoints.size}"))
            }

            // ✅ 长按删除最后一个点（再长按 2 秒可清空）
            setOnMapLongClickListener { latLng ->
                if (routePoints.isNotEmpty()) {
                    if (routePoints.size == 1) {
                        routePoints.clear()
                        clear()
                        Toast.makeText(context, "已删除最后一个点", Toast.LENGTH_SHORT).show()
                    } else {
                        // 删除最后一个点
                        routePoints.removeAt(routePoints.lastIndex)
                        clear()
                        if (routePoints.size >= 2) {
                            addPolyline(
                                PolylineOptions()
                                    .addAll(routePoints)
                                    .color(0xFF00E5FF.toInt())
                                    .width(8f)
                                    .useGradient(true)
                            )
                        }
                        routePoints.forEachIndexed { i, p ->
                            addMarker(MarkerOptions().position(p).title("点 ${i + 1}"))
                        }
                        Toast.makeText(context, "删除最后一个点（剩余 ${routePoints.size} 个）", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "当前没有可删除的点", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 状态变量（放在 Scaffold 内外都能访问）
    var query by remember { mutableStateOf("") }
    var tips by remember { mutableStateOf(listOf<Tip>()) }
    var noResult by remember { mutableStateOf(false) } // ✅ 是否无搜索结果
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("绘制路径(长按撤销点)", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (routePoints.size < 2) {
                            Toast.makeText(context, "请至少选两个点", Toast.LENGTH_SHORT).show()
                        } else {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("route_points", ArrayList(routePoints))
                            Toast.makeText(
                                context,
                                "路径已保存，共 ${routePoints.size} 个点",
                                Toast.LENGTH_SHORT
                            ).show()
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.Done, contentDescription = "完成", tint = Color.White)
                    }
                },
                modifier = Modifier.background(
                    Brush.horizontalGradient(listOf(Color(0xFF6A11CB), Color(0xFF2575FC)))
                ),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Transparent)
        ) {
            // 地图
            AndroidView(
                factory = { mapView },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
            )

                // 搜索输入框 + 搜索按钮（新版样式）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                        .clip(RoundedCornerShape(40.dp))
                        // ✅ 深色玻璃质感背景 + 强边框 + 立体阴影
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xCC6A11CB), // 主紫
                                    Color(0x882575FC)  // 较浅蓝
                                )
                            ),
                            shape = RoundedCornerShape(40.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.7f),
                                    Color(0xFF7B61FF).copy(alpha = 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(40.dp)
                        )
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(40.dp),
                            ambientColor = Color(0xFF6A11CB).copy(alpha = 0.25f),
                            spotColor = Color(0xFF2575FC).copy(alpha = 0.45f)
                        )

                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (it.length >= 2) {
                                scope.launch {
                                    if (isNetworkAvailable(context)) {
                                        val results = searchTipsOnline(query, "a03af5f20576f26a43f4b3b6249c2066")
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            tips = results.map { (name, latlon) ->
                                                Tip().apply {
                                                    this.name = name
                                                    this.setPostion(LatLonPoint(latlon.first, latlon.second))
                                                }
                                            }
                                            noResult = tips.isEmpty()
                                        }
                                    } else {
                                        Toast.makeText(context, "网络不可用", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                tips = emptyList()
                                noResult = false
                            }
                        },
                        placeholder = { Text("输入地名 / POI / 地标", color = Color.White.copy(alpha = 0.6f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        singleLine = true
                    )

                    // 渐变背景按钮
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
                                )
                            )
                            .clickable {
                                if (query.isNotBlank()) {
                                    scope.launch {
                                        if (!isNetworkAvailable(context)) {
                                            Toast.makeText(context, "请检查网络连接", Toast.LENGTH_SHORT).show()
                                        } else {
                                            try {
                                                val results = searchTipsOnline(query, "a03af5f20576f26a43f4b3b6249c2066")
                                                withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    tips = results.map { (name, latlon) ->
                                                        Tip().apply {
                                                            this.name = name
                                                            this.setPostion(LatLonPoint(latlon.first, latlon.second))
                                                        }
                                                    }
                                                    noResult = tips.isEmpty()
                                                    if (noResult) Toast.makeText(context, "未找到相关结果", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "请求失败：${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "请输入搜索内容", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text("搜索", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

            // 搜索结果
            if (tips.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 80.dp) // ✅ 搜索框下方（避免遮挡）
                        .zIndex(5f)           // ✅ 确保在地图上方显示
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            // ✅ 3D 玻璃背景
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xEEFFFFFF),
                                        Color(0xCCF0F4FF)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.9f),
                                        Color(0xFF7B61FF).copy(alpha = 0.3f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color.Black.copy(alpha = 0.15f),
                                spotColor = Color(0xFF7B61FF).copy(alpha = 0.25f)
                            )
                            .padding(vertical = 6.dp)
                            .heightIn(max = 220.dp) // ✅ 限制结果高度（避免占太多空间）
                    ) {
                        items(tips) { tip ->
                            tip.point?.let { point ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            aMap?.animateCamera(
                                                com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(point.latitude, point.longitude),
                                                    17f
                                                )
                                            )
                                            tips = emptyList()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tip.name,
                                        color = Color(0xFF222222),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Divider(
                                    color = Color(0x22000000),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

                // ✅ 无搜索结果提示
                if (noResult && query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未找到相关地点，请尝试输入其他关键词",
                            color = Color(0xFF555555),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
