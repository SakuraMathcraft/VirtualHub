package com.virtual.tensorhub.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.virtual.tensorhub.R

@Composable
fun AboutScreen(onBack: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 启动时触发动画
    LaunchedEffect(Unit) {
        visible = true
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
                    onClick = onBack,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack, // Or Icons.AutoMirrored.Filled.ArrowBack if preferred
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        // 🌟 整个内容加入渐入 + 弹性缩放动画
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(400)) +
                    scaleIn(
                        initialScale = 0.9f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = 200f
                        )
                    ),
            exit = fadeOut(animationSpec = tween(250)) +
                    scaleOut(
                        targetScale = 0.9f,
                        animationSpec = tween(250)
                    ),
             modifier = Modifier.padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部信息卡（App 图标 + 简介）
                IOSGlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // App Icon Placeholder
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .shadow(10.dp, RoundedCornerShape(22.dp))
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.primary) // App Color
                                .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(22.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                             Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "TensorHub",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Virtual Location Simulator",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                             Text(
                                "v1.0.0 (Beta)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // 功能介绍
                IOSListGroup(title = "Information") {
                    ListItem(
                        headlineContent = { Text("Developer") },
                        supportingContent = { Text("Mathcraft") },
                        leadingContent = { Icon(Icons.Default.Info, null) }
                    )
                    HorizontalDivider(color = Color.LightGray.copy(alpha=0.2f))
                     ListItem(
                        headlineContent = { Text("Contact") },
                        supportingContent = { Text("https://github.com/Septemc/TensorHubCommunity") },
                        leadingContent = { Icon(Icons.AutoMirrored.Filled.Send, null) }
                    )
                }
                
                // Disclaimer
                 IOSListGroup(title = "Disclaimer") {
                    Text(
                        text = "This application is for educational and testing purposes only. Please do not use it for illegal activities. The developer assumes no responsibility for any misuse.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                 }
                 
                  
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/* -------------------- ↓ 可复用组件 ↓ -------------------- */
@Composable
fun GlassButton(
    text: String,
    icon: ImageVector,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isLoading) 360f else 0f,
        animationSpec = if (isLoading)
            infiniteRepeatable(animation = tween(800, easing = LinearEasing))
        else
            tween(300),
        label = "rotateAnim"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.25f))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .graphicsLayer { rotationZ = rotation }
                    .size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.White)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f))
        Text(value, color = Color.White)
    }
}

@Composable
fun AppCard(
    iconRes: Int,
    name: String,
    desc: String,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 500f
        ),
        label = "appCardScale"
    )
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable {
                scope.launch {
                    pressed = true
                    kotlinx.coroutines.delay(90)
                    pressed = false
                }
            }
            .padding(14.dp)
            .height(IntrinsicSize.Min), // ✅ 确保高度一致
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = name,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // ✅ 限制标题不换行并截断溢出字符
        Text(
            text = name,
            color = Color.White,
            maxLines = 1,                             // 限制一行
            softWrap = false,                         // 不换行
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis // 超出用省略号
        )

        // ✅ 描述也设定行数，确保高度一致
        Text(
            text = desc,
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,                              // 最多两行
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
@Composable
fun GlassButtonDynamic(
    text: String,
    icon: ImageVector,
    isLoading: Boolean = false,
    success: Boolean = false,
    onClick: () -> Unit
) {
    // 平滑过渡背景颜色
    val bgColor by animateColorAsState(
        targetValue = when {
            success -> Color(0xFF4CAF50).copy(alpha = 0.65f)  // 成功 → 绿色半透明
            isLoading -> Color.White.copy(alpha = 0.25f)
            else -> Color.White.copy(alpha = 0.25f)
        },
        animationSpec = tween(400),
        label = "buttonBgColor"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isLoading) 360f else 0f,
        animationSpec = if (isLoading)
            infiniteRepeatable(animation = tween(800, easing = LinearEasing))
        else
            tween(300),
        label = "rotateAnim"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .graphicsLayer { rotationZ = rotation }
                    .size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
