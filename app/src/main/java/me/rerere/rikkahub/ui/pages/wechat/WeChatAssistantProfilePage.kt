package me.rerere.rikkahub.ui.pages.wechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowLeft01
import me.rerere.hugeicons.stroke.Brain02
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.ui.components.ui.UIAvatar
import me.rerere.rikkahub.ui.context.LocalNavController
import org.koin.compose.koinInject

/**
 * 微信风格的「角色资料」页。
 *
 * 对应微信里点开联系人看到的资料卡：大头像、昵称、个性签名（人设），
 * 这里是「人机恋」最关键的一页——对面 AI 长什么样、叫什么、是什么性格，都在这里定。
 *
 * 上游已经有一个功能齐全的 AssistantDetailPage（Material 风格），
 * 本页是它的微信风格精简版，只保留调教人设最常用的四项：头像、名字、人设、记忆入口。
 * 其余高级设置仍可从上首页进入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeChatAssistantProfilePage(id: String) {
    val settingsStore: SettingsStore = koinInject()
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()

    // settingsFlow 本身是 StateFlow，有初始值，不需要 initialValue
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val assistant: Assistant? = remember(settings, id) {
        settings.assistants.firstOrNull { it.id.toString() == id }
    }

    // 名字和人设是本地编辑态，点保存才写回，避免每敲一个字就落库
    var name by remember(assistant?.id) { mutableStateOf(assistant?.name.orEmpty()) }
    var prompt by remember(assistant?.id) { mutableStateOf(assistant?.systemPrompt.orEmpty()) }

    fun save(transform: (Assistant) -> Assistant) {
        val current = assistant ?: return
        scope.launch {
            settingsStore.update { s ->
                s.copy(
                    assistants = s.assistants.map {
                        if (it.id == current.id) transform(it) else it
                    }
                )
            }
        }
    }

    Scaffold(
        containerColor = WeChatColors.ListBackground,
        topBar = {
            TopAppBar(
                title = { Text("角色资料", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    Icon(
                        imageVector = HugeIcons.ArrowLeft01,
                        contentDescription = "返回",
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(22.dp)
                            .clickable { navController.popBackStack() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WeChatColors.ListBackground
                )
            )
        }
    ) { paddings ->
        if (assistant == null) {
            Column(
                modifier = Modifier
                    .padding(paddings)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("角色不存在", color = WeChatColors.TextSecondary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(paddings)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 头像：点一下换图/换 emoji，UIAvatar 自带选图和裁剪
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UIAvatar(
                    name = name,
                    value = assistant.avatar,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    onUpdate = { avatar -> save { it.copy(avatar = avatar) } }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "点头像可以换照片",
                    style = MaterialTheme.typography.bodySmall,
                    color = WeChatColors.TextSecondary
                )
            }

            HorizontalDivider(color = WeChatColors.Divider, thickness = 0.5.dp)

            // 昵称
            ProfileField(
                label = "昵称",
                value = name,
                placeholder = "她叫什么",
                onValueChange = { name = it }
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = WeChatColors.Divider,
                thickness = 0.5.dp
            )

            // 人设
            ProfileField(
                label = "人设",
                value = prompt,
                placeholder = "她是谁，什么性格，怎么跟你说话……",
                singleLine = false,
                onValueChange = { prompt = it }
            )

            // 保存按钮
            val dirty = name != assistant.name || prompt != assistant.systemPrompt
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (dirty) WeChatColors.Green else WeChatColors.Divider
                    )
                    .clickable(enabled = dirty) {
                        save { it.copy(name = name.trim(), systemPrompt = prompt) }
                    }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (dirty) "保存" else "已保存",
                    color = if (dirty) androidx.compose.ui.graphics.Color.White
                    else WeChatColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = WeChatColors.Divider, thickness = 0.5.dp)

            // 记忆入口：她的「记忆星海」
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate(Screen.AssistantMemory(id = assistant.id.toString()))
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = HugeIcons.Brain02,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = WeChatColors.TextSecondary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "记忆星海",
                        style = MaterialTheme.typography.bodyLarge,
                        color = WeChatColors.TextPrimary
                    )
                    Text(
                        text = "看看她记住了你什么",
                        style = MaterialTheme.typography.bodySmall,
                        color = WeChatColors.TextSecondary
                    )
                }
                Text("›", style = MaterialTheme.typography.titleLarge, color = WeChatColors.TextSecondary)
            }

            HorizontalDivider(color = WeChatColors.Divider, thickness = 0.5.dp)

            // 完整设置：跳到上游功能齐全的详情页
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate(Screen.AssistantDetail(id = assistant.id.toString()))
                    }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "更多设置",
                        style = MaterialTheme.typography.bodyLarge,
                        color = WeChatColors.TextPrimary
                    )
                    Text(
                        text = "模型、温度、工具、正则等",
                        style = MaterialTheme.typography.bodySmall,
                        color = WeChatColors.TextSecondary
                    )
                }
                Text("›", style = MaterialTheme.typography.titleLarge, color = WeChatColors.TextSecondary)
            }
        }
    }
}

/**
 * 资料页里的一行输入。
 *
 * 微信的资料卡是「标签在左、内容在右」的样式，这里保持一致；
 * 人设是多行，所以 singleLine 可关。
 */
@Composable
private fun ProfileField(
    label: String,
    value: String,
    placeholder: String,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = WeChatColors.TextPrimary,
            modifier = Modifier.width(64.dp)
        )
        // 单行和多行分开写：singleLine = true 时如果同时传 minLines > 1，
        // material3 内部会因参数冲突直接抛异常，所以两条分支各写各的
        val fieldModifier = Modifier.weight(1f)
        val placeholderBlock: @Composable () -> Unit = {
            Text(placeholder, style = MaterialTheme.typography.bodyMedium)
        }
        if (singleLine) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = fieldModifier,
                placeholder = placeholderBlock,
                singleLine = true,
                shape = RoundedCornerShape(6.dp)
            )
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = fieldModifier,
                placeholder = placeholderBlock,
                singleLine = false,
                minLines = 5,
                shape = RoundedCornerShape(6.dp)
            )
        }
    }
}