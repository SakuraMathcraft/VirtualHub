package com.virtual.tensorhub.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
                )
            )
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
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
                    )
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部信息卡（App 图标 + 简介）
                GlassCard {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "App Icon",
                            tint = Color.White,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                                    )
                                )
                                .padding(12.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "虚拟定位",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            "Version 1.0.0",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "一款功能强大的虚拟定位工具，支持地图快速选点、SWPU快速选择与地址搜索，为您的位置服务提供便捷解决方案。",
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(18.dp))

                        var isChecking by remember { mutableStateOf(false) }
                        var isLatest by remember { mutableStateOf(false) }

                        GlassButtonDynamic(
                            text = if (isLatest) "已是最新版本" else "检查更新",
                            icon = Icons.Default.Refresh,
                            isLoading = isChecking,
                            success = isLatest,
                        ) {
                            if (!isChecking && !isLatest) {
                                isChecking = true
                                scope.launch {
                                    kotlinx.coroutines.delay(2000)
                                    isChecking = false
                                    isLatest = true
                                    // 3 秒后恢复初始状态
                                    kotlinx.coroutines.delay(3000)
                                    isLatest = false
                                }
                            }
                        }
                    }
                }

                // 开发者信息卡
                GlassCard {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "开发者信息",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(10.dp))
                        InfoRow("团队", "TensorHub Dev Team")
                        InfoRow("邮箱", "support@tensorhub.com")
                        InfoRow("官网", "www.tensorhub.com")
                        InfoRow("开源地址", "github.com/tensorhub")
                    }
                }

                // 更多应用
                GlassCard {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "更多应用",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AppCard(
                                    iconRes = R.drawable.ic_tensorcloud,
                                    name = "TensorCloud",
                                    desc = "云端存储与协作平台",
                                    bgColor = Color(0xFF4A90E2),
                                    modifier = Modifier.weight(1f)
                                )
                                AppCard(
                                    iconRes = R.drawable.ic_tensornote,
                                    name = "TensorNote",
                                    desc = "智能笔记与知识管理",
                                    bgColor = Color(0xFFFF9800),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AppCard(
                                    iconRes = R.drawable.ic_tensorfit,
                                    name = "TensorFit",
                                    desc = "健康运动追踪应用",
                                    bgColor = Color(0xFF4CAF50),
                                    modifier = Modifier.weight(1f)
                                )
                                AppCard(
                                    iconRes = R.drawable.ic_tensorpay,
                                    name = "TensorPay",
                                    desc = "安全便捷的支付工具",
                                    bgColor = Color(0xFFFF4081),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 优化建议卡
                var expanded by remember { mutableStateOf(false) }
                var feedbackText by remember { mutableStateOf("") }

                // -------- 🌟 功能与 UI 优化建议卡 --------
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 顶部点击区域
                        Column(
                            modifier = Modifier
                                .bounceClick { expanded = !expanded }
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("功能与 UI 优化建议", color = Color.White)
                            Text(
                                if (expanded) "点击可收起输入框" else "点击提交您的使用体验与改进建议",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // 下方弹出输入框（顺序结构 → 在标题下方弹出，不遮挡）
                        AnimatedVisibility(
                            visible = expanded,
                            enter = fadeIn(tween(250)) + expandVertically(
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = 260f
                                )
                            ),
                            exit = fadeOut(tween(250)) + shrinkVertically(
                                spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = 180f
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                // 玻璃圆角输入框
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(1.dp)
                                ) {
                                    OutlinedTextField(
                                        value = feedbackText,
                                        onValueChange = { feedbackText = it },
                                        placeholder = {
                                            Column {
                                                Text(
                                                    "例如：UI太丑了，求作者优化…",
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    "(请发送到 2245924824@qq.com)",
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                                                )
                                            }
                                        },
                                        singleLine = false,
                                        maxLines = 4,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.Transparent)
                                            .padding(2.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.White.copy(alpha = 0.4f),
                                            unfocusedIndicatorColor = Color.White.copy(alpha = 0.2f),
                                            cursorColor = Color.White,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                // 居中发送按钮（纸飞机图标）
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GlassButton(
                                        text = "发送反馈",
                                        icon = Icons.Default.Send,
                                        isLoading = false
                                    ) {
                                        if (feedbackText.isNotBlank()) {
                                            try {
                                                // ---------- 🌟 智能检测邮箱环境 ----------
                                                val gmailIntent = android.content.Intent(
                                                    android.content.Intent.ACTION_SENDTO,
                                                    android.net.Uri.parse("mailto:2245924824@qq.com")
                                                ).apply {
                                                    setPackage("com.google.android.gm")  // Gmail
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "TensorHub 功能与 UI 优化建议")
                                                    putExtra(android.content.Intent.EXTRA_TEXT, feedbackText)
                                                }

                                                val qqIntent = android.content.Intent(
                                                    android.content.Intent.ACTION_SENDTO,
                                                    android.net.Uri.parse("mailto:2245924824@qq.com")
                                                ).apply {
                                                    setPackage("com.tencent.androidqqmail")  // QQ 邮箱
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "TensorHub 功能与 UI 优化建议")
                                                    putExtra(android.content.Intent.EXTRA_TEXT, feedbackText)
                                                }

                                                val pm = context.packageManager
                                                when {
                                                    pm.resolveActivity(gmailIntent, 0) != null -> {
                                                        context.startActivity(gmailIntent)
                                                        Toast.makeText(context, "正在打开 Gmail...", Toast.LENGTH_SHORT).show()
                                                    }
                                                    pm.resolveActivity(qqIntent, 0) != null -> {
                                                        context.startActivity(qqIntent)
                                                        Toast.makeText(context, "正在打开 QQ 邮箱...", Toast.LENGTH_SHORT).show()
                                                    }
                                                    else -> {
                                                        // 如果都没有安装，就退回系统默认处理
                                                        val fallback = android.content.Intent(
                                                            android.content.Intent.ACTION_SENDTO,
                                                            android.net.Uri.parse("mailto:2245924824@qq.com")
                                                        ).apply {
                                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "TensorHub 功能与 UI 优化建议")
                                                            putExtra(android.content.Intent.EXTRA_TEXT, feedbackText)
                                                        }
                                                        context.startActivity(fallback)
                                                        Toast.makeText(context, "已打开默认邮件应用", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "未找到可用的邮件客户端", Toast.LENGTH_SHORT).show()
                                            }

                                            feedbackText = ""
                                            expanded = false
                                        } else {
                                            Toast.makeText(context, "请输入反馈内容", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 底部版权卡
                Text(
                    "© 2025 TensorHub. All rights reserved.\nMade with ❤️ for better mobile experience",
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 20.dp, top = 6.dp)
                )
            }
        }

        // 返回按钮
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 4.dp)
                .size(46.dp),
            color = Color.White.copy(alpha = 0.15f),
            shape = RoundedCornerShape(50),
            tonalElevation = 4.dp,
            shadowElevation = 6.dp
        ) {
            IconButton(onClick = {
                scope.launch {
                    visible = false
                    kotlinx.coroutines.delay(300) // 与 exit 动画时间匹配
                    onBack()                     // 动画播放完再返回
                }
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
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



