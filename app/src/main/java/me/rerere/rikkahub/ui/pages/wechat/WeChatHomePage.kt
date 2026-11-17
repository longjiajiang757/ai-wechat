package me.rerere.rikkahub.ui.pages.wechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import kotlinx.coroutines.launch
import me.rerere.hugeicons.stroke.Message01
import me.rerere.hugeicons.stroke.MessageAdd01
import me.rerere.hugeicons.stroke.Search01
import me.rerere.hugeicons.stroke.Setting07
import me.rerere.hugeicons.stroke.UserCircle
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.ui.components.ui.UIAvatar
import me.rerere.rikkahub.ui.context.LocalNavController
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 微信风格的四栏主界面。
 *
 * 用 NavigationBar 承载「微信 / 通讯录 / 发现 / 我」四个 tab，
 * 每个 tab 内部是独立的内容区；聊天页仍然复用上游的 ChatPage，
 * 避免重写流式输出、消息分支、工具调用这些已有能力。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeChatHomePage() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var pendingNewChat by remember { mutableStateOf(false) }
    val viewModel: WeChatHomeVM = koinViewModel()
    val navController = LocalNavController.current
    val assistants by viewModel.assistants.collectAsStateWithLifecycle()

    // 发起新会话：先切到选中的角色，再用随机 UUID 建新会话
    // （上游约定：随机 UUID 即新会话，initializeConversation 会读取 settings.assistantId）
    val scope = rememberCoroutineScope()
    val startNewChatWith: (Assistant) -> Unit = { assistant ->
        scope.launch {
            viewModel.selectAssistant(assistant.id)
            navController.navigate(
                me.rerere.rikkahub.Screen.Chat(
                    id = kotlin.uuid.Uuid.random().toString()
                )
            )
        }
    }

    if (pendingNewChat) {
        AssistantPickDialog(
            assistants = assistants,
            title = "和谁聊",
            onDismiss = { pendingNewChat = false },
            onPick = { assistant ->
                pendingNewChat = false
                startNewChatWith(assistant)
            }
        )
    }

    Scaffold(
        containerColor = WeChatColors.ListBackground,
        topBar = {
            // 微信顶部标题随 tab 变化，只有「微信」页有搜索按钮
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "微信"
                            1 -> "通讯录"
                            2 -> "发现"
                            else -> "我"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    if (selectedTab == 0) {
                        Icon(
                            imageVector = HugeIcons.Search01,
                            contentDescription = "搜索",
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(22.dp)
                        )
                        Icon(
                            imageVector = HugeIcons.MessageAdd01,
                            contentDescription = "发起会话",
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(22.dp)
                                .clickable { pendingNewChat = true }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WeChatColors.ListBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = WeChatColors.ListBackground,
                tonalElevation = 0.dp
            ) {
                WeChatTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WeChatColors.TabActive,
                            selectedTextColor = WeChatColors.TabActive,
                            unselectedIconColor = WeChatColors.TabInactive,
                            unselectedTextColor = WeChatColors.TabInactive,
                            indicatorColor = WeChatColors.ListBackground
                        )
                    )
                }
            }
        }
    ) { paddings ->
        Box(
            modifier = Modifier
                .padding(paddings)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> WeChatConversationTab(viewModel)
                1 -> WeChatContactsTab(viewModel)
                2 -> WeChatDiscoverTab(viewModel)
                else -> WeChatMeTab()
            }
        }
    }
}

/** 底部四个 tab，顺序和微信一致 */
private enum class WeChatTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Chat("微信", HugeIcons.Message01),
    Contacts("通讯录", HugeIcons.UserCircle),
    Discover("发现", HugeIcons.Search01),
    Me("我", HugeIcons.Setting07),
}

/**
 * 微信 tab：会话列表。
 *
 * 一行 = 头像 + 昵称 + 最后一条消息 + 时间，点进去打开聊天页。
 */
@Composable
private fun WeChatConversationTab(viewModel: WeChatHomeVM) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val navController = LocalNavController.current

    if (conversations.isEmpty()) {
        EmptyHint(
            title = "还没有聊天",
            subtitle = "去「通讯录」挑一个角色开始吧"
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = conversations,
            key = { it.conversation.id.toString() }
        ) { item ->
            ConversationRow(
                item = item,
                onClick = {
                    navController.navigate(
                        me.rerere.rikkahub.Screen.Chat(id = item.conversation.id.toString())
                    )
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.5.dp,
                color = WeChatColors.Divider
            )
        }
    }
}

/** 会话列表的一行 */
@Composable
private fun ConversationRow(
    item: WeChatConversationItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(WeChatColors.ListBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像用角色头像；角色为空时给个占位
        UIAvatar(
            name = item.assistant?.name.orEmpty(),
            value = item.assistant?.avatar ?: me.rerere.rikkahub.data.model.Avatar.Dummy,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.conversation.title.ifBlank {
                        item.assistant?.name.orEmpty().ifBlank { "新会话" }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = WeChatColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatWeChatTime(item.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = WeChatColors.TextSecondary
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = item.lastMessagePreview.ifBlank { "暂无消息" },
                style = MaterialTheme.typography.bodySmall,
                color = WeChatColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 通讯录 tab：把每个角色当成一个「联系人」。
 *
 * 点进去打开角色的人设编辑页（复用上游 AssistantDetail），
 * 在那里可以改头像、名字、人设提示词——这就是「人机恋」里对面 AI 的形象设置入口。
 */
@Composable
private fun WeChatContactsTab(viewModel: WeChatHomeVM) {
    val assistants by viewModel.assistants.collectAsStateWithLifecycle()
    val navController = LocalNavController.current

    if (assistants.isEmpty()) {
        EmptyHint(title = "还没有角色", subtitle = "去设置里新建一个角色")
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = assistants, key = { it.id.toString() }) { assistant ->
            ContactRow(
                assistant = assistant,
                onClick = {
                    navController.navigate(
                        me.rerere.rikkahub.Screen.AssistantDetail(id = assistant.id.toString())
                    )
                }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.5.dp,
                color = WeChatColors.Divider
            )
        }
    }
}

/** 通讯录的一行 */
@Composable
private fun ContactRow(assistant: Assistant, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(WeChatColors.ListBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UIAvatar(
            name = assistant.name,
            value = assistant.avatar,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = assistant.name.ifBlank { "未命名角色" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = WeChatColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (assistant.systemPrompt.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = assistant.systemPrompt.replace(Regex("\\s+"), " ").take(40),
                    style = MaterialTheme.typography.bodySmall,
                    color = WeChatColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 发现 tab：记忆星海入口 */
@Composable
private fun WeChatDiscoverTab(viewModel: WeChatHomeVM) {
    val navController = LocalNavController.current
    val assistants by viewModel.assistants.collectAsStateWithLifecycle()
    var pendingMemoryPick by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        DiscoverEntry(
            title = "记忆星海",
            subtitle = if (assistants.isEmpty()) "先建一个角色" else "点进去选角色，看她记住了你什么",
            onClick = {
                // 记忆是按角色隔离的，只有一个角色时直接进，多个时先选
                when {
                    assistants.isEmpty() -> Unit
                    assistants.size == 1 -> navController.navigate(
                        me.rerere.rikkahub.Screen.AssistantMemory(id = assistants.first().id.toString())
                    )
                    else -> pendingMemoryPick = true
                }
            }
        )
        HorizontalDivider(color = WeChatColors.Divider, thickness = 0.5.dp)
    }

    if (pendingMemoryPick) {
        AssistantPickDialog(
            assistants = assistants,
            title = "看谁的记忆",
            onDismiss = { pendingMemoryPick = false },
            onPick = { assistant ->
                pendingMemoryPick = false
                navController.navigate(
                    me.rerere.rikkahub.Screen.AssistantMemory(id = assistant.id.toString())
                )
            }
        )
    }
}

/** 选角色的小弹窗，记忆和新建会话都用它 */
@Composable
private fun AssistantPickDialog(
    assistants: List<Assistant>,
    title: String,
    onDismiss: () -> Unit,
    onPick: (Assistant) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(items = assistants, key = { it.id.toString() }) { assistant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(assistant) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UIAvatar(
                            name = assistant.name,
                            value = assistant.avatar,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = assistant.name.ifBlank { "未命名角色" },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun DiscoverEntry(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(WeChatColors.ListBackground)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = WeChatColors.TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = WeChatColors.TextSecondary
            )
        }
        Text(text = "›", style = MaterialTheme.typography.titleLarge, color = WeChatColors.TextSecondary)
    }
}

/** 我 tab：设置入口 */
@Composable
private fun WeChatMeTab() {
    val navController = LocalNavController.current
    Column(modifier = Modifier.fillMaxSize()) {
        DiscoverEntry(
            title = "设置",
            subtitle = "模型、API Key、外观",
            onClick = {
                navController.navigate(me.rerere.rikkahub.Screen.Setting)
            }
        )
        HorizontalDivider(color = WeChatColors.Divider, thickness = 0.5.dp)
        DiscoverEntry(
            title = "支持",
            subtitle = "作者信息与下载地址",
            onClick = {
                navController.navigate(me.rerere.rikkahub.Screen.SettingDonate)
            }
        )
        HorizontalDivider(color = WeChatColors.Divider, thickness = 0.5.dp)
    }
}

/** 空状态提示 */
@Composable
private fun EmptyHint(title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = WeChatColors.TextSecondary)
            Spacer(Modifier.height(6.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = WeChatColors.TextSecondary)
        }
    }
}

/** 微信风格的时间：今天显示时分，昨天显示「昨天」，更早显示日期 */
private fun formatWeChatTime(millis: Long): String {
    if (millis <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - millis
    val dayMillis = 24 * 60 * 60 * 1000L
    return when {
        diff < dayMillis && isSameDay(millis, now) ->
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
        diff < 2 * dayMillis -> "昨天"
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(millis))
    }
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return fmt.format(Date(a)) == fmt.format(Date(b))
}
