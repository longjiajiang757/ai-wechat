package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
 * 全局背景图：一张图铺在所有聊天页底下，跟角色自带的背景是两层。
 *
 * 层级从下到上：全局图 → 角色背景（AssistantBackground）→ 内容。
 * 角色自己设了背景图时，它画在上层，全局图自然被盖住，符合「角色设置优先」。
 *
 * 只在设置 → 主题 里选过图才画；没选直接返回，零开销。
 * 不透明度读 appBackgroundOpacity，默认 0.35，聊天文字才不会被花哨背景吃掉。
 */
@Composable
fun AppGlobalBackground(modifier: Modifier = Modifier) {
    val settingsStore = koinInject<SettingsStore>()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val background = settings.displaySetting.appBackground

    if (background.isNullOrBlank()) return

    val opacity = settings.displaySetting.appBackgroundOpacity.coerceIn(0f, 1f)
    Box(modifier = modifier.fillMaxSize()) {
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
