package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import me.rerere.rikkahub.ui.hooks.readStringPreference
import me.rerere.rikkahub.ui.hooks.writeStringPreference
import me.rerere.rikkahub.utils.Announcement
import me.rerere.rikkahub.utils.AnnouncementChecker
import org.koin.compose.koinInject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import me.rerere.rikkahub.utils.writeClipboardText

/**
 * 冷启动公告检查器。挂在主界面里，自己完成「拉取 → 比对已读 → 弹窗」。
 *
 * 只在聊天主页放一个即可（ChatPage 调用），进子页面不重复请求。
 */
@Composable
fun AnnouncementStartupCheck() {
    val context = LocalContext.current
    val checker: AnnouncementChecker = koinInject()
    var announcement by remember { mutableStateOf<Announcement?>(null) }
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 上次读过的公告 id；没读过任何公告时是 null
        val lastReadId = context.readStringPreference(KEY_READ_ANNOUNCEMENT, null)
        val fetched = withContext(Dispatchers.IO) { checker.fetch() }
        if (fetched != null && fetched.id != lastReadId) {
            announcement = fetched
            show = true
        }
    }

    if (show && announcement != null) {
        AnnouncementDialog(
            announcement = announcement!!,
            onDismiss = {
                show = false
                // 记下这条已读，下次冷启动不再弹；换 id 才会再弹
                context.writeStringPreference(KEY_READ_ANNOUNCEMENT, announcement!!.id)
            },
        )
    }
}

@Composable
fun AnnouncementViewDialog(
    announcement: Announcement?,
    onDismiss: () -> Unit,
) {
    if (announcement == null) return
    AnnouncementDialog(announcement = announcement, onDismiss = onDismiss)
}

@Composable
private fun AnnouncementDialog(
    announcement: Announcement,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var showJoined by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(announcement.title, style = MaterialTheme.typography.titleLarge)
                if (announcement.publishedAt.isNotBlank()) {
                    Text(
                        text = announcement.publishedAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MarkdownBlock(
                    content = announcement.body,
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalDivider()
                if (showJoined) {
                    Text(
                        text = "群号 $QQ_GROUP_NUMBER 已复制，打开 QQ 搜索加入群聊",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        // 一键加群需要群主申请 joinqun key，我们没有；
                        // joinQQGroup(null) 会拼出无效链接，所以直接复制群号，
                        // 用户打开 QQ 搜群号加入。
                        context.writeClipboardText(QQ_GROUP_NUMBER)
                        showJoined = true
                    },
                ) {
                    Text("复制群号加群")
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text("知道了")
                }
            }
        },
    )
}

/** 已读公告 id 的存储键：换公告时改 announcements.json 的 id 即可再次全员弹窗 */
private const val KEY_READ_ANNOUNCEMENT = "read_announcement_id_v1"

/** 交流群号，展示与复制用 */
const val QQ_GROUP_NUMBER = "1131115406"
