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
import com.amap.api.maps.model.LatLng
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
import androidx.compose.material.icons.filled.LocationOn
import com.virtual.tensorhub.mock.MockLocationService

// ================================================================
// 🧊 历史位置界面：默认分组 + 添加到组 + 新建/删除
// ================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // ---------- 初始化默认分组 ----------
    var groups by remember { mutableStateOf(loadGroupsWithDefaults(context)) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    
    // Group Picker state
    var showGroupPicker by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<HistoryItem?>(null) }


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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Or Icons.Filled.ArrowBack
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "History & Favorites",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                 Spacer(modifier = Modifier.weight(1f))
                 IOSButton(
                    onClick = { showAddDialog = true },
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Group",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(groups) { group ->
                IOSListGroup(title = group.name) {
                    if (group.locations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No items yet",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        group.locations.forEachIndexed { index, item ->
                            if (index > 0) HorizontalDivider(color = Color.LightGray.copy(alpha=0.2f), modifier = Modifier.padding(start = 16.dp))
                            
                            ListItem(
                                headlineContent = { Text(item.name, fontWeight = FontWeight.Medium) },
                                supportingContent = { 
                                    Text(
                                        "${String.format("%.4f", item.latitude)}, ${String.format("%.4f", item.longitude)}", 
                                        color = Color.Gray, 
                                        fontSize = 12.sp
                                    ) 
                                },
                                leadingContent = {
                                     Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                         Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                },
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            group.locations.remove(item)
                                            // Trigger recompose manually or simply update state
                                            val newGroups = groups.map { if (it === group) it.copy(locations = group.locations) else it }
                                            groups = newGroups
                                            saveGroups(context, groups)
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = Color.LightGray) // Subtle delete
                                    }
                                },
                                modifier = Modifier.clickable {
                                    // Use location
                                     navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("pickedLocation", LatLng(item.latitude, item.longitude))
                                    navController.popBackStack()
                                    Toast.makeText(context, "Selected: ${item.name}", Toast.LENGTH_SHORT).show()
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
            
            item {
                TextButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Text("Clear All History", color = Color.Red)
                }
            }
        }
    }

    // Add Group Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Group") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Group Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotEmpty()) {
                            val newGroups = groups.toMutableList()
                            newGroups.add(GroupItem(newGroupName, mutableListOf()))
                            groups = newGroups
                            saveGroups(context, groups)
                            newGroupName = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Confirm Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All?") },
            text = { Text("This will delete all saved locations.") },
            confirmButton = {
                TextButton(
                    onClick = {
                         val defaults = loadGroupsWithDefaults(context).filter { it.name != "历史记录" } // Keep defaults maybe? or reset all
                         // Assuming reset to defaults
                         val reset = listOf(GroupItem("历史记录", mutableListOf())) + 
                             listOf("校园", "公司", "家").map { GroupItem(it, mutableListOf()) }
                         groups = reset
                         saveGroups(context, groups)
                         showClearConfirm = false
                    }
                ) {
                    Text("Delete All", color = Color.Red)
                }
            },
            dismissButton = {
                 TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
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
                                val item = selectedItem!!
                                val exists = g.locations.any {
                                    it.latitude == item.latitude && it.longitude == item.longitude
                                }

                                if (exists) {
                                    Toast.makeText(context, "该地址已存在于 ${g.name}", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }

                                g.locations.add(item)
                                groups = groups.map { it.copy(locations = it.locations.toMutableList()) }
                                saveGroups(context, groups)
                                groups = groups.toMutableList() // ✅ 强制刷新
                                Toast.makeText(context, "已将地址添加到 ${g.name}", Toast.LENGTH_SHORT).show()
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

// Add helper function to save history
fun saveHistory(context: Context, name: String, address: String?, latLng: LatLng) {
    val groups = loadGroupsWithDefaults(context)
    val historyGroup = groups.find { it.name == "历史记录" } 
        ?: groups.firstOrNull() 
        ?: return

    // Avoid duplicates
    if (historyGroup.locations.none { it.latitude == latLng.latitude && it.longitude == latLng.longitude }) {
        historyGroup.locations.add(0, HistoryItem(name, latLng.latitude, latLng.longitude))
        saveGroups(context, groups)
    }
}
