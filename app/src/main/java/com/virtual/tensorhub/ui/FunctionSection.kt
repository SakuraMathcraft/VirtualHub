package com.virtual.tensorhub.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FunctionSection(
    onMapClick: () -> Unit = {},
    onGetRealLocation: () -> Unit = {},
    onStopMock: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onCollectClick: () -> Unit = {},   // 历史/收藏入口
    onRouteClick: () -> Unit = {},
    onMockStarted: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // ===== 第一行：主功能（大按钮，突出显示） =====
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButtonLarge(
                text = "打开地图选点",
                icon = Icons.Filled.Map,
                onClick = onMapClick
            )

            GlassButtonLarge(
                text = "路径模拟(测试)",
                icon = Icons.Filled.AltRoute,
                onClick = {
                    onRouteClick()
                    onMockStarted()
                }
            )
        }

        // ===== 第二行：辅助功能（三个小按钮） =====
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            GlassButtonSmall(
                text = "停止虚拟定位",
                icon = Icons.Filled.Stop,
                onClick = onStopMock
            )

            GlassButtonSmall(
                text = "地址搜索",
                icon = Icons.Filled.Search,
                onClick = onSearchClick
            )

            GlassButtonSmall(
                text = "历史位置",
                icon = Icons.Filled.History,
                onClick = onCollectClick
            )
        }
    }
}

/* ---------- 大按钮：主功能，带弹性点击反馈 ---------- */
@Composable
fun RowScope.GlassButtonLarge(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "largeButtonScale"
    )
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .weight(1f)
            .height(90.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.06f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.28f),
                shape = shape
            )
            .clickable {
                coroutineScope.launch {
                    pressed = true
                    delay(90)
                    pressed = false
                    onClick()
                }
            }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp
            )
        }
    }
}

/* ---------- 小按钮：次要功能，带弹性点击反馈 ---------- */
@Composable
fun RowScope.GlassButtonSmall(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "smallButtonScale"
    )
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .weight(1f)
            .height(70.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(
                width = 0.8.dp,
                color = Color.White.copy(alpha = 0.22f),
                shape = shape
            )
            .clickable {
                coroutineScope.launch {
                    pressed = true
                    delay(90)
                    pressed = false
                    onClick()
                }
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 11.sp
            )
        }
    }
}
