@file:OptIn(ExperimentalLayoutApi::class)

package com.virtual.tensorhub.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.amap.api.maps.model.LatLng
import com.virtual.tensorhub.mock.MockLocationProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

@Composable
fun AddressSearchScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var tipList by remember { mutableStateOf(listOf<Tip>()) }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
                )
            )
            .padding(top = statusBarPadding + 12.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // ===== 搜索栏 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .defaultMinSize(minHeight = 52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮
                Surface(
                    modifier = Modifier.size(46.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // 输入框 + 搜索按钮
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(40.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (it.length >= 2) {
                                // 🔄 启动协程实时搜索
                                scope.launch {
                                    val tips = searchTipsOnline(query, "a03af5f20576f26a43f4b3b6249c2066")
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        tipList = tips.map { (name, latlng) ->
                                            Tip(name = name, point = LatLonPoint(latlng.first, latlng.second))
                                        }
                                    }
                                }
                            } else tipList = emptyList()
                        },
                        placeholder = {
                            Text("输入地址/POI/地标", color = Color.White.copy(alpha = 0.6f))
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = Color.White,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp),
                        singleLine = true
                    )

                    // 搜索按钮
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
                                    if (!isNetworkAvailable(context)) {
                                        Toast.makeText(context, "请检查网络连接", Toast.LENGTH_SHORT).show()
                                        // 👉 跳转系统网络设置界面（可选）
                                        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                                        return@clickable
                                    }

                                    scope.launch {
                                        try {
                                            val tips = searchTipsOnline(query, "a03af5f20576f26a43f4b3b6249c2066")
                                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                if (tips.isEmpty()) {
                                                    Toast.makeText(context, "未找到相关结果", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    tipList = tips.map { (name, latlng) ->
                                                        Tip(name = name, point = LatLonPoint(latlng.first, latlng.second))
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                Toast.makeText(context, "请求失败，请检查网络或API Key", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "请输入搜索内容", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text("搜索", color = Color.White)
                    }
                }
            }

            // ===== 搜索结果列表 =====
            if (tipList.isNotEmpty()) {
                GlassCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "搜索结果",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(10.dp))
                        tipList.forEach { tip ->
                            if (tip.point != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.3f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            val point = tip.point
                                            if (point != null) {
                                                val lat = point.latitude
                                                val lng = point.longitude

                                                saveVirtualLocation(context, lat, lng)
                                                val intent = Intent(context, com.virtual.tensorhub.mock.MockLocationService::class.java).apply {
                                                    action = com.virtual.tensorhub.mock.MockLocationService.ACTION_START
                                                    putExtra(com.virtual.tensorhub.mock.MockLocationService.EXTRA_LAT, lat)
                                                    putExtra(com.virtual.tensorhub.mock.MockLocationService.EXTRA_LON, lng)
                                                }
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                                                    context.startForegroundService(intent)
                                                else
                                                    context.startService(intent)

                                                // ✅ 自动保存到历史记录
                                                val groups = loadGroups(context).toMutableList()
                                                val defaultGroup = groups.find { it.name == "历史记录" } ?: GroupItem("历史记录", mutableListOf())
                                                defaultGroup.locations.add(
                                                    HistoryItem(
                                                        name = tip.name,
                                                        latitude = lat,
                                                        longitude = lng
                                                    )
                                                )
                                                if (!groups.contains(defaultGroup)) groups.add(defaultGroup)
                                                saveGroups(context, groups)

                                                reverseGeocode(context, lat, lng) { addr ->
                                                    Toast.makeText(context, "已设置：$addr", Toast.LENGTH_SHORT).show()
                                                }

                                                navController.previousBackStackEntry
                                                    ?.savedStateHandle
                                                    ?.set("pickedLocation", LatLng(lat, lng))
                                                navController.popBackStack()
                                            }
                                        }
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = tip.name ?: "未知名称",
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        if (!tip.address.isNullOrEmpty()) {
                                            Text(
                                                text = tip.address,
                                                color = Color.White.copy(alpha = 0.7f),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // ===== 热门位置 =====
            GlassCard {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "热门位置",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(12.dp))
                    val hotList = listOf("油砖", "成都理工大学", "纽约", "天安门", "故宫", "颐和园", "成都春熙路", "天府大道林心如", "必吃榜")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        hotList.forEach { name ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.3f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        scope.launch {
                                            val tips = searchTipsOnline(name, "a03af5f20576f26a43f4b3b6249c2066")
                                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                if (tips.isEmpty()) {
                                                    Toast.makeText(context, "未找到相关结果", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    tipList = tips.map { (tName, latlng) ->
                                                        Tip(name = tName, point = LatLonPoint(latlng.first, latlng.second))
                                                    }
                                                }
                                            }
                                            query = name
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(name, color = Color.White)
                            }
                        }
                    }
                }
            }

            // ===== 搜索提示 =====
            GlassCard {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "使用提示",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• 输入地名或POI将自动联想\n" +
                                "• 点击结果可立即设置虚拟定位\n" +
                                "• 支持中文模糊搜索",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp)
                    )
                }
            }
        }
    }
}
/**
 * 🔍 使用高德 Web API 实现输入提示（无需 SDK）
 */
suspend fun searchTipsOnline(
    keyword: String,
    amapKey: String
): List<Pair<String, Pair<Double, Double>>> = withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val url =
            "https://restapi.amap.com/v3/assistant/inputtips?key=$amapKey&keywords=$keyword&datatype=all"
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val code = connection.responseCode
        val text = connection.inputStream.bufferedReader().use { it.readText() }

        Log.d("AddressSearch", "HTTP响应码: $code")
        Log.d("AddressSearch", "返回内容: $text")

        val json = org.json.JSONObject(text)
        if (json.optString("status") != "1") {
            Log.e("AddressSearch", "API调用失败: ${json.optString("info")}")
            return@withContext emptyList()
        }

        val tips = json.optJSONArray("tips") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until tips.length()) {
                val t = tips.getJSONObject(i)
                val name = t.optString("name")
                val loc = t.optString("location")
                if (loc.isNotEmpty() && loc.contains(",")) {
                    val parts = loc.split(",")
                    val lng = parts[0].toDoubleOrNull() ?: continue
                    val lat = parts[1].toDoubleOrNull() ?: continue
                    add(name to (lat to lng))
                } else {
                    Log.w("AddressSearch", "跳过无坐标结果: $name")
                }
            }
        }
    } catch (e: Exception) {
        Log.e("AddressSearch", "网络错误: ${e.message}", e)
        emptyList()
    }
}

data class Tip(
    var name: String = "",
    var address: String = "",
    var point: LatLonPoint? = null
)

data class LatLonPoint(
    val latitude: Double,
    val longitude: Double
)
fun isNetworkAvailable(context: android.content.Context): Boolean {
    val connectivityManager =
        context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val actNw = connectivityManager.getNetworkCapabilities(network) ?: return false
    return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}


