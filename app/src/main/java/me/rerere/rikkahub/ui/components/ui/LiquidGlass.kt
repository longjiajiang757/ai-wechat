package me.rerere.rikkahub.ui.components.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * 液态玻璃主题的公共件。
 *
 * 只用本仓库已验证能编译的 API，逐个标注先例出处：
 * - Path().addOutline(shape.createOutline(...)) + clipPath：ChatMessage.kt:624 clipDrawing
 * - drawRect(brush)：ChatInput.kt:224
 * - Modifier.border(BorderStroke(w, color), shape)：Markdown.kt:366
 * - spring(dampingRatio=, stiffness=)：AsrButton.kt:200
 * - graphicsLayer { scaleX/scaleY }：ReorderableDrag.kt:62
 * - collectIsPressedAsState：ChatMessageAttachments.kt:42
 *
 * 没用 Stroke / compositeOver —— 这两个在本仓库零先例，不猜。
 */

/**
 * 玻璃面板顶部的内反光带：裁在形状里铺一条上亮下透的渐变，
 * 模拟环境光从上方打下来。配合 [Modifier.border] 的亮边就是完整的镜面感。
 */
fun Modifier.liquidSheen(
    shape: Shape,
    sheenColor: Color = Color.White,
    sheenAlpha: Float = 0.12f,
    bandRatio: Float = 0.38f,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    val bandHeight = size.height * bandRatio
    val brush = Brush.verticalGradient(
        colors = listOf(sheenColor.copy(alpha = sheenAlpha), Color.Transparent),
        startY = 0f,
        endY = bandHeight,
    )
    onDrawWithContent {
        drawContent()
        clipPath(path) { drawRect(brush = brush) }
    }
}

/** 玻璃边缘的亮边配色：上亮下暗，环境光从顶部来 */
@Composable
fun liquidRimStroke(highlight: Color, alpha: Float = 0.5f): BorderStroke =
    BorderStroke(1.dp, highlight.copy(alpha = alpha))

/**
 * 液态按压形变：按下去整体缩小一点，spring 回弹带果冻感。
 *
 * 缩放走 graphicsLayer，不影响布局；圆角收拢由调用方用
 * [liquidPressProgress] 自己 lerp（和 ChatMessage 的气泡同一套路）。
 */
@Composable
fun Modifier.liquidPress(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.975f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        // 数值照 AsrButton：dampingRatio 0.6f 微弹、stiffness 400f 偏软 → 果冻回弹
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 380f,
        ),
        label = "liquid_press_scale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** 按压进度 State（0..1），spring 曲线，用于圆角/透明度等连续形变 */
@Composable
fun liquidPressProgress(interactionSource: MutableInteractionSource): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = 380f,
        ),
        label = "liquid_press_progress",
    )
}