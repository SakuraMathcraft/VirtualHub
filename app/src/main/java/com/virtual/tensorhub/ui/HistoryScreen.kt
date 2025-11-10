package com.virtual.tensorhub.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.virtual.tensorhub.mock.MockLocationProvider
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.draw.blur
import android.content.Intent
import android.os.Build
import com.virtual.tensorhub.mock.MockLocationService

// ================================================================
// 🧊 历史位置界面：默认分组 + 添加到组 + 新建/删除
// ================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // ---------- 初始化默认分组 ----------
    var groups by remember { mutableStateOf(loadGroupsWithDefaults(context)) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    var showGroupPicker by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ===== 顶部栏 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "历史位置(再次打开时生效)",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
            }

            // ===== 分组列表 =====
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(groups) { group ->
                    GlassCard {
                        Column(Modifier.padding(16.dp)) {
                            // 分组标题
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${group.name} (${group.locations.size})",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }
                                // 删除分组按钮（默认四个分组不能删）
                                if (group.name !in listOf("历史记录", "校园", "公司", "家")) {
                                    IconButton(onClick = {
                                        groups = (groups - group).map { it.copy(locations = it.locations.toMutableList()) }
                                        saveGroups(context, groups)
                                    }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "删除分组",
                                            tint = Color(0xFFFF7B7B)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // 分组内的历史记录
                            group.locations.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            // 保存选择的坐标
                                            saveVirtualLocation(context, item.latitude, item.longitude)

                                            // 启动前台服务保持长期注入
                                            val intent = Intent(context, MockLocationService::class.java).apply {
                                                action = MockLocationService.ACTION_START
                                                putExtra(MockLocationService.EXTRA_LAT, item.latitude)
                                                putExtra(MockLocationService.EXTRA_LON, item.longitude)
                                            }
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(intent)
                                            } else {
                                                context.startService(intent)
                                            }

                                            reverseGeocode(context, item.latitude, item.longitude) { addr ->
                                                Toast.makeText(context, "已跳转至：$addr", Toast.LENGTH_SHORT).show()
                                            }

                                            navController.popBackStack()
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.name, color = Color.White, fontSize = 16.sp)
                                        Text(
                                            "${String.format("%.4f° N", item.latitude)}, ${String.format("%.4f° E", item.longitude)}",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 13.sp
                                        )
                                    }
                                    Row {
                                        // “添加到组”功能仅在历史记录组出现
                                        if (group.name == "历史记录") {
                                            IconButton(onClick = {
                                                selectedItem = item
                                                showGroupPicker = true
                                            }) {
                                                Icon(
                                                    Icons.Default.Folder,
                                                    contentDescription = "添加到分组",
                                                    tint = Color(0xFF91E8FF)
                                                )
                                            }
                                        }
                                        // 删除记录
                                        IconButton(onClick = {
                                            group.locations.remove(item)
                                            groups = groups.map { it.copy(locations = it.locations.toMutableList()) } // ✅ 彻底触发重组
                                            saveGroups(context, groups)
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "删除记录",
                                                tint = Color(0xFFFF7B7B)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // ===== 新建分组按钮 =====
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { showAddDialog = true }
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("新建分组", color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

// ===== 新建分组 BottomSheet 弹窗（统一风格）=====
    if (showAddDialog) {
        ModalBottomSheet(
            containerColor = Color.Transparent,
            onDismissRequest = { showAddDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.3f),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text(
                    "新建分组",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(16.dp))

                // 🧊 玻璃输入框
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.35f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        placeholder = {
                            Text(
                                "请输入分组名称",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = Color(0xFF91E8FF),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("取消", color = Color.White)
                    }
                    TextButton(onClick = {
                        if (newGroupName.isNotBlank()) {
                            groups = groups + GroupItem(newGroupName, mutableListOf())
                            saveGroups(context, groups)
                            newGroupName = ""
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, "名称不能为空", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("确定", color = Color(0xFF91E8FF), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ===== 添加到分组弹窗 =====
    if (showGroupPicker && selectedItem != null) {
        ModalBottomSheet(
            containerColor = Color.Transparent,
            onDismissRequest = { showGroupPicker = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))
                        ),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(20.dp)
            ) {
                Text("添加到分组", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                groups.filter { it.name != "历史记录" }.forEach { g ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .clickable {
                                g.locations.add(selectedItem!!)
                                groups = groups.map { it.copy(locations = it.locations.toMutableList()) }
                                saveGroups(context, groups)
                                Toast.makeText(context, "已添加到 ${g.name}", Toast.LENGTH_SHORT).show()
                                showGroupPicker = false
                            }
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    ) {
                        Text(g.name, color = Color.White, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ================================================================
// 🧠 数据模型与存取逻辑
// ================================================================
data class GroupItem(
    val name: String,
    val locations: MutableList<HistoryItem>
)

data class HistoryItem(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

fun loadGroupsWithDefaults(context: Context): List<GroupItem> {
    val saved = loadGroups(context).toMutableList()
    val defaults = listOf("历史记录", "校园", "公司", "家")
    defaults.forEach { name ->
        if (saved.none { it.name == name }) saved.add(GroupItem(name, mutableListOf()))
    }
    saveGroups(context, saved)
    return saved.sortedBy { if (it.name == "历史记录") 0 else 1 }
}

fun loadGroups(context: Context): List<GroupItem> {
    val sp = context.getSharedPreferences("virtual_prefs", Context.MODE_PRIVATE)
    val json = sp.getString("history_groups", "[]") ?: "[]"
    val arr = JSONArray(json)
    val groups = mutableListOf<GroupItem>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val name = obj.optString("name")
        val locArr = obj.optJSONArray("locations") ?: JSONArray()
        val locs = mutableListOf<HistoryItem>()
        for (j in 0 until locArr.length()) {
            val loc = locArr.getJSONObject(j)
            locs.add(
                HistoryItem(
                    name = loc.optString("name"),
                    latitude = loc.optDouble("lat"),
                    longitude = loc.optDouble("lng")
                )
            )
        }
        groups.add(GroupItem(name, locs))
    }
    return groups
}

fun saveGroups(context: Context, groups: List<GroupItem>) {
    val arr = JSONArray()
    groups.forEach { g ->
        val locArr = JSONArray()
        g.locations.forEach { l ->
            locArr.put(JSONObject().apply {
                put("name", l.name)
                put("lat", l.latitude)
                put("lng", l.longitude)
            })
        }
        arr.put(JSONObject().apply {
            put("name", g.name)
            put("locations", locArr)
        })
    }
    val sp = context.getSharedPreferences("virtual_prefs", Context.MODE_PRIVATE)
    sp.edit().putString("history_groups", arr.toString()).apply()
}
