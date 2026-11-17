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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import me.rerere.rikkahub.ui.context.LocalSettings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import me.rerere.hugeicons.stroke.Add01

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
 * 点进去打开微信风格的角色资料页（头像/昵称/人设），
 * 在那里可以改头像、名字、人设提示词——这就是「人机恋」里对面 AI 的形象设置入口。
 */
@Composable
private fun WeChatContactsTab(viewModel: WeChatHomeVM) {
    val assistants by viewModel.assistants.collectAsStateWithLifecycle()
    val navController = LocalNavController.current

    if (assistants.isEmpty()) {
        // 新装用户第一眼看到的就是这里，光写「去设置里新建」等于没说：
        // 用户根本不知道设置里哪一项。直接给一个能点的按钮，一步到新建页。
        EmptyAssistantGuide(
            onAdd = { navController.navigate(me.rerere.rikkahub.Screen.Assistant) }
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "__add_assistant__") {
            AddAssistantRow(
                onClick = { navController.navigate(me.rerere.rikkahub.Screen.Assistant) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 76.dp), thickness = 0.5.dp, color = WeChatColors.Divider)
        }
        items(items = assistants, key = { it.id.toString() }) { assistant ->
            ContactRow(
                assistant = assistant,
                onClick = {
                    navController.navigate(
                        me.rerere.rikkahub.Screen.WeChatAssistantProfile(id = assistant.id.toString())
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

/**
 * 还没有角色时的引导页。
 *
 * 新装用户第一眼就是这里。原来只写一行「去设置里新建一个角色」，
 * 可用户既不知道设置在哪里，也不知道设置里哪一项，等于把人卡死在第一步。
 * 这里直接给一个绿色主按钮，点了就进角色列表页，那里有「+」。
 */
@Composable
private fun EmptyAssistantGuide(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "还没有角色",
            style = MaterialTheme.typography.titleMedium,
            color = WeChatColors.TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "角色就是你要聊的那个人：给她起个名字、写一段人设、换个头像。",
            style = MaterialTheme.typography.bodySmall,
            color = WeChatColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = WeChatColors.Green),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("新建角色", color = Color.White)
        }
    }
}

/** 有角色时列表顶部的新增入口，微信里就是「新的朋友」那一行的位置 */
@Composable
private fun AddAssistantRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(WeChatColors.ListBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = HugeIcons.Add01,
                contentDescription = null,
                tint = WeChatColors.Green,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "新建角色",
            style = MaterialTheme.typography.bodyLarge,
            color = WeChatColors.TextPrimary
        )
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
                        me.rerere.rikkahub.Screen.WeChatMemory(id = assistants.first().id.toString())
                    )
                    else -> pendingMemoryPick = true
                }
            }
        )
        HorizontalDivider(color = WeChatColors.Divider, thickness = 0.5.dp)

        DiscoverEntry(
            title = "插件市场",
            subtitle = "给她加点性格：早安、深夜、拍照分享……",
            onClick = { navController.navigate(me.rerere.rikkahub.Screen.WeChatPluginMarket) }
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
                    me.rerere.rikkahub.Screen.WeChatMemory(id = assistant.id.toString())
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

/**
 * 我 tab。
 *
 * 顶部放作者信息（头像 + 名字 + QQ），下面是功能入口。
 * 之前这里只有「设置」「支持」两项，角色管理藏在设置的三级菜单里，
 * 用户加完服务商之后完全找不到新建角色的地方，所以把「角色管理」提到这一层。
 */
@Composable
private fun WeChatMeTab() {
    val navController = LocalNavController.current
    val settings = LocalSettings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(WeChatColors.ListBackground)
    ) {
        // 头部：和微信「我」一样，一块浅色区域放头像和名字
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WeChatColors.ListBackground)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UIAvatar(
                    name = settings.userNickname.ifBlank { "我" },
                    value = settings.userAvatar,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = settings.userNickname.ifBlank { "我" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WeChatColors.TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "AI agent · 嘉酱",
                        style = MaterialTheme.typography.bodySmall,
                        color = WeChatColors.TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "QQ 2924574099",
                style = MaterialTheme.typography.bodySmall,
                color = WeChatColors.TextSecondary
            )
        }

        HorizontalDivider(color = WeChatColors.Divider, thickness = 8.dp)

        DiscoverEntry(
            title = "角色管理",
            subtitle = "新建、编辑人设和头像",
            onClick = { navController.navigate(me.rerere.rikkahub.Screen.Assistant) }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = WeChatColors.Divider, thickness = 0.5.dp)
        DiscoverEntry(
            title = "设置",
            subtitle = "服务商、API Key、外观",
            onClick = { navController.navigate(me.rerere.rikkahub.Screen.Setting) }
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = WeChatColors.Divider, thickness = 0.5.dp)
        DiscoverEntry(
            title = "支持",
            subtitle = "作者信息与下载地址",
            onClick = { navController.navigate(me.rerere.rikkahub.Screen.SettingDonate) }
        )
        HorizontalDivider(color = WeChatColors.Divider, thickness = 8.dp)
        Spacer(Modifier.height(24.dp))
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
