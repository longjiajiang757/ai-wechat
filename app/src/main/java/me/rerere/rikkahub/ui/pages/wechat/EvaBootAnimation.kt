package me.rerere.rikkahub.ui.pages.wechat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * EVA 二号机风格的启动动画。
 *
 * 整个动画分三段，总长约 1.8 秒：
 * 1. 0.0~0.5s  橙色警戒网格铺开，边角出现同步标记
 * 2. 0.5~1.3s  中央机体轮廓线逐渐亮起，同步率数字快速上涨
 * 3. 1.3~1.8s  闪光 + 「EVA-02 启动」字样，然后整块淡出
 *
 * 故意不用 Bitmap：全部是 Canvas 画线和文字，包里不增加图片体积，
 * 也保证在任何屏幕密度上都清晰。
 */
private val EvaOrange = Color(0xFFFF6B1A)
private val EvaOrangeDim = Color(0xFF8A3A0E)
private val EvaGreen = Color(0xFF6BFF8F)
private val EvaBlack = Color(0xFF07080A)

/** 启动动画总时长，调用方用它决定什么时候移除覆盖层 */
internal const val EVA_BOOT_DURATION_MS = 1900L

@Composable
fun EvaBootAnimation(
    modifier: Modifier = Modifier,
    onFinished: () -> Unit,
) {
    // 0→1 的主进度，线性推进，各元素自己按需要做缓动
    val progress = remember { Animatable(0f) }
    var syncRate by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(EVA_BOOT_DURATION_MS.toInt(), easing = LinearEasing))
        onFinished()
    }

    // 同步率跟着进度涨，末尾卡在 100.0% 更有仪式感
    LaunchedEffect(Unit) {
        while (syncRate < 1000) {
            delay(16)
            syncRate = (progress.value * 1000).toInt().coerceAtMost(1000)
        }
    }

    val p = progress.value
    // 前 15% 淡入，最后 15% 淡出，中间常亮
    val alpha = when {
        p < 0.12f -> p / 0.12f
        p > 0.86f -> ((1f - p) / 0.14f).coerceIn(0f, 1f)
        else -> 1f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EvaBlack),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // ---- 背景：橙色径向光晕，像仪表盘从中心亮起 ----
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        EvaOrange.copy(alpha = 0.20f * alpha),
                        Color.Transparent
                    ),
                    center = Offset(w / 2, h * 0.42f),
                    radius = w * 0.85f
                )
            )

            // ---- 六边形网格：EVA 界面标志性的蜂窝底纹 ----
            val hexR = w / 9f
            val gridAlpha = 0.16f * alpha * (p * 1.6f).coerceAtMost(1f)
            var row = 0
            var cy = -hexR
            while (cy < h + hexR) {
                var cx = if (row % 2 == 0) -hexR else 0f
                while (cx < w + hexR) {
                    val path = Path()
                    for (i in 0 until 6) {
                        val ang = Math.PI / 3 * i + Math.PI / 6
                        val x = cx + (hexR * 0.62f * Math.cos(ang)).toFloat()
                        val y = cy + (hexR * 0.62f * Math.sin(ang)).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                    drawPath(path, EvaOrangeDim.copy(alpha = gridAlpha), style = Stroke(width = 1.2f))
                    cx += hexR * 1.08f
                }
                cy += hexR * 0.94f
                row++
            }

            // ---- 四角对准框：像设备取景器的括角 ----
            val cornerLen = w * 0.10f
            val inset = w * 0.07f
            val cornerAlpha = ((p - 0.05f) / 0.25f).coerceIn(0f, 1f) * alpha
            val strokeW = 3.5f
            // 左上
            drawLine(EvaOrange.copy(alpha = cornerAlpha), Offset(inset, inset), Offset(inset + cornerLen, inset), strokeW)
            drawLine(EvaOrange.copy(alpha = cornerAlpha), Offset(inset, inset), Offset(inset, inset + cornerLen), strokeW)
            // 右上
            drawLine(EvaOrange.copy(alpha = cornerAlpha), Offset(w - inset, inset), Offset(w - inset - cornerLen, inset), strokeW)
            drawLine(EvaOrange.copy(alpha = cornerAlpha), Offset(w - inset, inset), Offset(w - inset, inset + cornerLen), strokeW)
            // 左下
            drawLine(EvaOrange.copy(alpha = cornerAlpha), Offset(inset, h - inset), Offset(inset + cornerLen, h - inset), strokeW)
            drawLine(EvaOrange.copy(alpha = cornerAlpha), Offset(inset, h - inset), Offset(inset, h - inset - cornerLen), strokeW)
            // 右下
            drawLine(EvaOrange.copy(alpha = cornerAlpha), Offset(w - inset, h - inset), Offset(w - inset - cornerLen, h - inset), strokeW)
            drawLine(EvaOrange.copy(alpha = cornerAlpha), Offset(w - inset, h - inset), Offset(w - inset, h - inset - cornerLen), strokeW)

            // ---- 中央机体轮廓：两条对称的折线，像二号机侧脸 ----
            val faceP = ((p - 0.22f) / 0.40f).coerceIn(0f, 1f)
            if (faceP > 0f) {
                val cxm = w / 2
                val cym = h * 0.44f
                val scale = w * 0.30f
                val faceAlpha = faceP * alpha
                // 右半侧轮廓（左半侧镜像）
                val half = Path().apply {
                    moveTo(cxm, cym - scale * 0.95f)
                    lineTo(cxm + scale * 0.42f, cym - scale * 0.30f)
                    lineTo(cxm + scale * 0.62f, cym + scale * 0.18f)
                    lineTo(cxm + scale * 0.30f, cym + scale * 0.72f)
                    lineTo(cxm + scale * 0.10f, cym + scale * 0.95f)
                }
                val left = Path().apply {
                    moveTo(cxm, cym - scale * 0.95f)
                    lineTo(cxm - scale * 0.42f, cym - scale * 0.30f)
                    lineTo(cxm - scale * 0.62f, cym + scale * 0.18f)
                    lineTo(cxm - scale * 0.30f, cym + scale * 0.72f)
                    lineTo(cxm - scale * 0.10f, cym + scale * 0.95f)
                }
                // 只画已经"长出来"的部分：用路径长度近似，直接靠 alpha 渐显更稳
                drawPath(half, EvaOrange.copy(alpha = faceAlpha), style = Stroke(width = 4f))
                drawPath(left, EvaOrange.copy(alpha = faceAlpha), style = Stroke(width = 4f))

                // 眼睛：两道斜向绿光
                val eyeP = ((p - 0.5f) / 0.3f).coerceIn(0f, 1f)
                if (eyeP > 0f) {
                    val eyeAlpha = eyeP * alpha
                    val bright = EvaGreen.copy(alpha = eyeAlpha)
                    drawLine(
                        bright,
                        Offset(cxm - scale * 0.46f, cym - scale * 0.10f),
                        Offset(cxm - scale * 0.12f, cym + scale * 0.02f),
                        6f
                    )
                    drawLine(
                        bright,
                        Offset(cxm + scale * 0.46f, cym - scale * 0.10f),
                        Offset(cxm + scale * 0.12f, cym + scale * 0.02f),
                        6f
                    )
                }
            }

            // ---- 扫描线：从中间往外扫，制造"通电"感 ----
            val scanP = ((p - 0.15f) / 0.55f).coerceIn(0f, 1f)
            if (scanP > 0f && scanP < 1f) {
                val y = h * 0.44f + (scanP - 0.5f) * 2 * h * 0.5f
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            EvaOrange.copy(alpha = 0.55f * alpha),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 2.5f
                )
            }

            // ---- 底部进度条 ----
            val barP = ((p - 0.10f) / 0.72f).coerceIn(0f, 1f)
            if (barP > 0f) {
                val barW = w * 0.62f
                val barH = 3f
                val bx = (w - barW) / 2
                val by = h * 0.80f
                drawRect(
                    color = EvaOrangeDim.copy(alpha = 0.5f * alpha),
                    topLeft = Offset(bx, by),
                    size = Size(barW, barH)
                )
                drawRect(
                    color = EvaOrange.copy(alpha = alpha),
                    topLeft = Offset(bx, by),
                    size = Size(barW * barP, barH)
                )
            }

            // ---- 结尾闪光 ----
            val flashP = ((p - 0.80f) / 0.20f).coerceIn(0f, 1f)
            if (flashP > 0f && flashP < 1f) {
                val fa = (1f - flashP) * 0.5f
                drawRect(color = EvaOrange.copy(alpha = fa))
            }
        }

        // ---- 文字层 ----
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(0.dp))

            // 顶部小字：状态行
            if (p > 0.18f) {
                Text(
                    text = "SYNC RATIO",
                    color = EvaOrange.copy(alpha = alpha * 0.85f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp
                )
            }

            Spacer(Modifier.height(220.dp))

            // 主标题：到 1.3s 之后才出现
            if (p > 0.68f) {
                val titleAlpha = ((p - 0.68f) / 0.14f).coerceIn(0f, 1f) * alpha
                Text(
                    text = "EVA-02",
                    color = EvaOrange.copy(alpha = titleAlpha),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 6.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "启动",
                    color = Color.White.copy(alpha = titleAlpha),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 8.sp
                )
            }
        }

        // 同步率数字贴在进度条上方
        if (p > 0.12f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "%d.%d".format(syncRate / 10, syncRate % 10),
                        color = EvaGreen.copy(alpha = alpha),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "%",
                        color = EvaGreen.copy(alpha = alpha * 0.8f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 右下角编号，像机体铭牌
        Text(
            text = "UNIT 02 // ASUKA",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 44.dp),
            color = EvaOrange.copy(alpha = alpha * 0.55f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
    }
}

/** 启动音效的资源名，交给调用方播放 */
internal const val EVA_BOOT_SOUND = "eva_boot"