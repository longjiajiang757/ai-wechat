package me.rerere.rikkahub.ui.pages.wechat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import me.rerere.rikkahub.data.datastore.SettingsStore
import org.koin.compose.koinInject

/**
 * 全局背景图。
 *
 * 只负责画图，不负责取图：调用方把它放在内容下面一层（Box 的第一个子项）即可。
 * 图片按 [ContentScale.Crop] 铺满，超出的部分裁掉，避免拉伸变形。
 *
 * 不透明度默认从设置里读，聊天页文字密，太透会看不清，所以默认压到 0.35。
 */
@Composable
fun WeChatBackground(
    modifier: Modifier = Modifier,
) {
    val settingsStore = koinInject<SettingsStore>()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val background = settings.displaySetting.appBackground

    if (background.isNullOrBlank()) return

    val opacity = settings.displaySetting.appBackgroundOpacity.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WeChatColors.ListBackground)
    ) {
        AsyncImage(
            model = background,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .alpha(opacity),
        )
    }
}
