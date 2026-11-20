package me.rerere.rikkahub.ui.components.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * 打字指示器：三个跳动的小圆点，放在对方气泡的位置，
 * 表示「她正在组织语言」。
 *
 * 相位用 sin 直接算，不用 tween(delayMillis)——后者在本仓库没有使用先例，
 * 参数名猜错就编译失败；sin 相位差是纯数学，零风险。
 * 动画骨架（rememberInfiniteTransition + infiniteRepeatable + tween +
 * LinearEasing + RepeatMode.Reverse）与 AsrButton 的脉冲完全同款。
 */
@Composable
fun TypingDots(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "typing_dots")
    // 0 → 2π 一个周期，各点按 120° 错开
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "typing_dots_phase",
    )
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val scales = remember { FloatArray(3) }
    for (i in 0..2) {
        scales[i] = (0.55 + 0.45 * (0.5 + 0.5 * sin(phase - i * (2.0 * PI / 3.0)))).toFloat()
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0..2) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(scales[i])
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.7f))
            )
        }
    }
}