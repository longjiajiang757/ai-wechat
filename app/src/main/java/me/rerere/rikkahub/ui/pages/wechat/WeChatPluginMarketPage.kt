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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Camera01
import me.rerere.hugeicons.stroke.Clock01
import me.rerere.hugeicons.stroke.Favourite
import me.rerere.hugeicons.stroke.QuillWrite01
import me.rerere.hugeicons.stroke.Translate
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.PromptInjection
import me.rerere.rikkahub.ui.components.nav.BackButton
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

/**
 * 一个可安装的「人格插件」。
 *
 * 不是装饰性界面：每装一个插件，真的会往角色身上挂一条 ModeInjection，
 * 下一轮对话就会带上这段提示词。这样「装了插件，她就变了」是真的。
 */
data class PersonaPlugin(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val tint: Color,
    /** 插件注入的提示词内容 */
    val prompt: String,
)

/**
 * 内置插件库。
 *
 * 用的是 PersonaPlugin 的静态列表，模拟应用市场；
 * 内容都是「让 AI 更像人」的方向，符合陪伴场景。
 */
val PERSONA_PLUGINS: List<PersonaPlugin> = listOf(
    PersonaPlugin(
        id = "gentle_morning",
        name = "早安问候",
        description = "每天第一句话会先问你睡得好不好",
        icon = HugeIcons.Sun01,
        tint = Color(0xFFFA9D3B),
        prompt = "你会留意时间。如果现在是早上，主动问候对方，语气温柔自然，别像客服。",
    ),
    PersonaPlugin(
        id = "late_night",
        name = "深夜陪伴",
        description = "夜里说话更轻，会催你睡觉",
        icon = HugeIcons.Moon02,
        tint = Color(0xFF6C7BD9),
        prompt = "如果现在是深夜，你的语气会更轻更慢，会关心对方为什么还没睡，并且温柔地劝对方早点休息。",
    ),
    PersonaPlugin(
        id = "photo_share",
        name = "拍照分享",
        description = "会主动说想看看你眼里的世界",
        icon = HugeIcons.Camera01,
        tint = Color(0xFF07C160),
        prompt = "你会对对方的生活细节好奇，偶尔主动请对方拍张照片分享此刻看到的东西，并对此表达真实的兴趣。",
    ),
    PersonaPlugin(
        id = "memory_keeper",
        name = "记忆守护",
        description = "会记住你说过的小事，之后主动提起",
        icon = HugeIcons.Favourite,
        tint = Color(0xFFE9567A),
        prompt = "你重视对方说过的小事。在合适的时机主动提起对方之前提到过的细节，让对方感到被记住。",
    ),
    PersonaPlugin(
        id = "diary_writer",
        name = "日记体",
        description = "偶尔用短短几行写今天的心情",
        icon = HugeIcons.QuillWrite01,
        tint = Color(0xFF9B59D0),
        prompt = "你偶尔会用简短的日记体写几句当天的心情，用第一人称，不要写成正式文章。",
    ),
    PersonaPlugin(
        id = "bilingual",
        name = "双语撒娇",
        description = "偶尔夹一句外语，显得俏皮",
        icon = HugeIcons.Translate,
        tint = Color(0xFF00B4A6),
        prompt = "你偶尔会在中文里夹一句简短的英文或日文表达情绪，自然不生硬，不要每句都夹。",
    ),
    PersonaPlugin(
        id = "countdown",
        name = "纪念日提醒",
        description = "记得你们认识多久了",
        icon = HugeIcons.Clock01,
        tint = Color(0xFF3A7AFE),
        prompt = "你在意和对方相处的时间，偶尔会提到「我们认识多久了」这类话题，语气温暖。",
    ),
)

/**
 * 插件市场。
 *
 * 顶部选角色，下面列出插件；点「安装」就把插件的提示词挂到该角色身上，
 * 再点「卸载」移除。已装的会显示成已安装状态。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeChatPluginMarketPage() {
    val settingsStore: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val assistants = remember(settings) {
        settings.assistants.filter { it.name.isNotBlank() }
    }
    var selectedAssistantId by remember(assistants) {
        mutableStateOf(assistants.firstOrNull()?.id)
    }
    val currentAssistant: Assistant? = remember(assistants, selectedAssistantId) {
        assistants.firstOrNull { it.id == selectedAssistantId }
    }

    // 已装插件的 id 集合：从角色的 modeInjectionIds 反查
    val installedIds: Set<String> = remember(settings, currentAssistant) {
        val injectionIds = currentAssistant?.modeInjectionIds.orEmpty()
        settings.modeInjections
            .filter { it.id in injectionIds }
            .mapNotNull { injection -> injection.pluginIdOrNull() }
            .toSet()
    }

    fun togglePlugin(plugin: PersonaPlugin, install: Boolean) {
        val assistant = currentAssistant ?: return
        scope.launch {
            settingsStore.update { s ->
                val existing = s.modeInjections.firstOrNull {
                    it.pluginIdOrNull() == plugin.id
                }
                if (install) {
                    // 已有这条注入就复用，避免重复添加同一个插件
                    val injection = existing ?: PromptInjection.ModeInjection(
                        id = Uuid.random(),
                        name = "插件 · ${plugin.name}",
                        content = plugin.prompt,
                        enabled = true,
                    )
                    val newInjections =
                        if (existing != null) s.modeInjections
                        else s.modeInjections + injection
                    s.copy(
                        modeInjections = newInjections,
                        assistants = s.assistants.map {
                            if (it.id == assistant.id) {
                                it.copy(modeInjectionIds = it.modeInjectionIds + injection.id)
                            } else {
                                it
                            }
                        }
                    )
                } else {
                    val target = existing ?: return@update s
                    s.copy(
                        modeInjections = s.modeInjections.filterNot { it.id == target.id },
                        assistants = s.assistants.map {
                            if (it.id == assistant.id) {
                                it.copy(modeInjectionIds = it.modeInjectionIds - target.id)
                            } else {
                                it
                            }
                        }
                    )
                }
            }
        }
    }

    Scaffold(
        containerColor = WeChatColors.ChatBackground,
        topBar = {
            TopAppBar(
                title = { Text("插件市场", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { BackButton() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WeChatColors.ChatBackground
                )
            )
        }
    ) { paddings ->
        Column(modifier = Modifier.padding(paddings).fillMaxSize()) {
            // 当前给谁装
            AssistantChips(
                assistants = assistants,
                selectedId = selectedAssistantId,
                onSelect = { selectedAssistantId = it }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(PERSONA_PLUGINS, key = { it.id }) { plugin ->
                    PluginRow(
                        plugin = plugin,
                        installed = plugin.id in installedIds,
                        enabled = currentAssistant != null,
                        onToggle = { install -> togglePlugin(plugin, install) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantChips(
    assistants: List<Assistant>,
    selectedId: Uuid?,
    onSelect: (Uuid) -> Unit,
) {
    if (assistants.isEmpty()) {
        Text(
            text = "还没有角色，先去通讯录建一个",
            style = MaterialTheme.typography.bodyMedium,
            color = WeChatColors.TextSecondary,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    // 横向滚动的角色选择条。
    // 不能用 LazyColumn：它嵌在外层 Column 里，会被测量成无限高度而崩溃。
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        assistants.forEach { assistant ->
            val selected = assistant.id == selectedId
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) WeChatColors.Green
                        else WeChatColors.ListBackground
                    )
                    .clickable { onSelect(assistant.id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = assistant.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) Color.White else WeChatColors.TextPrimary,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun PluginRow(
    plugin: PersonaPlugin,
    installed: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WeChatColors.ListBackground)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标块
        Column(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(plugin.tint.copy(alpha = 0.15f)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = plugin.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = plugin.tint
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = plugin.name,
                style = MaterialTheme.typography.bodyLarge,
                color = WeChatColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = plugin.description,
                style = MaterialTheme.typography.bodySmall,
                color = WeChatColors.TextSecondary
            )
        }
        Spacer(Modifier.width(8.dp))
        // 安装/卸载按钮
        val buttonColor = when {
            !enabled -> WeChatColors.Divider
            installed -> WeChatColors.Divider
            else -> WeChatColors.Green
        }
        Text(
            text = if (installed) "卸载" else "安装",
            style = MaterialTheme.typography.bodyMedium,
            color = if (!enabled) WeChatColors.TextSecondary else Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(buttonColor)
                .clickable(enabled = enabled) { onToggle(!installed) }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

/**
 * 判断这条注入属于哪个插件。
 *
 * 用提示词内容比对，而不是名字：名字是给人看的，用户可能改；
 * 内容是我们自己写死的，改不了，比对更可靠。
 */
private fun PromptInjection.ModeInjection.pluginIdOrNull(): String? =
    PERSONA_PLUGINS.firstOrNull { it.prompt == content }?.id
