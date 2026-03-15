@file:OptIn(ExperimentalLayoutApi::class)

package com.virtual.tensorhub.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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

import com.amap.api.services.help.Tip

@Composable
fun AddressSearchScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var tipList by remember { mutableStateOf(listOf<Tip>()) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background) // Match background or slightly translucent
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)), // Gray-ish
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            BasicTextField(
                                value = query,
                                onValueChange = { newValue ->
                                    query = newValue
                                    if (newValue.isNotEmpty()) {
                                        // 触发搜索
                                        val inputQuery = com.amap.api.services.help.InputtipsQuery(newValue, "")
                                        val inputTips = com.amap.api.services.help.Inputtips(context, inputQuery)
                                        inputTips.setInputtipsListener { list, rCode ->
                                            if (rCode == 1000 && list != null) {
                                                tipList = list
                                            }
                                        }
                                        inputTips.requestInputtipsAsyn()
                                    } else {
                                        tipList = emptyList()
                                    }
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (query.isEmpty()) {
                                        Text(
                                            "Search for a place or address",
                                            color = Color.Gray,
                                            fontSize = 17.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = ""; tipList = emptyList() },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel, // Filled circle X
                                        contentDescription = "Clear",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    // Cancel / Back Button
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary, // Apple Blue
                        fontSize = 17.sp,
                        modifier = Modifier
                            .clickable { navController.popBackStack() }
                            .padding(4.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
             verticalArrangement = Arrangement.spacedBy(0.dp) // Apple lists have dividers
        ) {
            if (tipList.isEmpty() && query.isEmpty()) {
                 item {
                     // 🚀 新增：热门商圈推荐（恢复原来卡片样式并美化）
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    ) {
                         Text(
                            text = "Popular Areas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // 预设热门地点
                        val hotSpots = listOf(
                            "春熙路" to LatLng(30.6586, 104.0648),
                            "太古里" to LatLng(30.6555, 104.0799),
                            "天府广场" to LatLng(30.6574, 104.0658), 
                            "宽窄巷子" to LatLng(30.6635, 104.0533),
                            "锦里" to LatLng(30.6468, 104.0494),
                            "环球中心" to LatLng(30.5695, 104.0628) 
                        )
                        
                         // Grid layout manually
                         hotSpots.chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach { (name, latLng) ->
                                     Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha=0.7f),
                                                        MaterialTheme.colorScheme.tertiary.copy(alpha=0.7f)
                                                    )
                                                )
                                            )
                                            .clickable {
                                                 navController.previousBackStackEntry
                                                    ?.savedStateHandle
                                                    ?.set("pickedLocation", latLng)
                                                  saveHistory(context, name, "Popular Spot", latLng)
                                                  navController.popBackStack()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                if (row.size == 1) { // Fill space if odd number
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                         }
                    }
                 
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray.copy(alpha = 0.3f),
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Search for locations", color = Color.Gray)
                        }
                    }
                 }
            } else {
                items(tipList) { tip ->
                    // 过滤掉没有坐标的点
                    if (tip.point != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // 保存选中位置 并返回
                                    val point = tip.point
                                    val latLng = LatLng(point.latitude, point.longitude)
                                    
                                    // 1. 保存到 navController
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("pickedLocation", latLng)

                                    // 2. 保存到历史记录
                                    saveHistory(context, tip.name, tip.district, latLng)

                                    navController.popBackStack()
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tip.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!tip.district.isNullOrEmpty()) {
                                        Text(
                                            text = tip.district,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 48.dp), // Indented divider
                            color = Color.Gray.copy(alpha = 0.2f)
                        )
                    }
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

fun isNetworkAvailable(context: android.content.Context): Boolean {
    val connectivityManager =
        context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val actNw = connectivityManager.getNetworkCapabilities(network) ?: return false
    return actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
fun safeLoadGroups(context: android.content.Context): MutableList<GroupItem> {
    return try {
        // 调用原始加载逻辑（若异常直接捕获）
        loadGroups(context)?.toMutableList() ?: mutableListOf()
    } catch (e: Exception) {
        Log.e("AddressSearch", "首次加载历史记录文件失败: ${e.message}")
        mutableListOf()
    }
}
