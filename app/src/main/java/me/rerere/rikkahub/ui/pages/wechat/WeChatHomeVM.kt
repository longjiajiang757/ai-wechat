package me.rerere.rikkahub.ui.pages.wechat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.repository.ConversationRepository
import kotlin.uuid.Uuid

/**
 * 一条微信风格的会话条目：会话本身 + 它所属的角色 + 最后一条消息摘要。
 *
 * 微信列表要同时显示头像（来自角色）、昵称（角色名）、最后一条消息和时间，
 * 所以不能直接用 Conversation，需要把这几样拼在一起。
 */
data class WeChatConversationItem(
    val conversation: Conversation,
    val assistant: Assistant?,
    val lastMessagePreview: String,
    val lastMessageTime: Long,
)

/**
 * 微信主界面（会话列表 / 通讯录 / 记忆入口）的数据来源。
 *
 * 只读：新建会话、发消息都走 ChatService 和 ConversationRepository 原有接口，
 * 这里不复制业务逻辑，避免和上游行为不一致。
 */
class WeChatHomeVM(
    private val settingsStore: SettingsStore,
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    /**
     * 会话列表。
     *
     * 微信只有一个会话列表，不按助手隔离，所以取所有助手的会话再合并排序；
     * 角色（Assistant）按 id 映射，用于列表头像和昵称。
     */
    val conversations: StateFlow<List<WeChatConversationItem>> =
        settingsStore.settingsFlow
            .flatMapLatest { settings ->
                val assistants = settings.assistants.associateBy { it.id }
                val flows: List<Flow<List<Conversation>>> = settings.assistants.map { assistant ->
                    conversationRepository.getConversationsOfAssistant(assistant.id)
                }
                if (flows.isEmpty()) {
                    flowOf<List<WeChatConversationItem>>(emptyList())
                } else {
                    // 逐个 combine 成一条 Flow<List<Conversation>>，避免 vararg 重载的类型推断歧义
                    val merged: Flow<List<Conversation>> = flows.reduce { acc, flow ->
                        combine(acc, flow) { a, b -> a + b }
                    }
                    merged.map { conversations ->
                        conversations
                            .map { conversation ->
                                WeChatConversationItem(
                                    conversation = conversation,
                                    assistant = assistants[conversation.assistantId],
                                    lastMessagePreview = conversation.lastMessagePreview(),
                                    lastMessageTime = conversation.updateAt.toEpochMilli(),
                                )
                            }
                            .sortedWith(
                                compareByDescending<WeChatConversationItem> { it.conversation.isPinned }
                                    .thenByDescending { it.lastMessageTime }
                            )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** 通讯录用的角色列表（去掉未命名的占位角色） */
    val assistants: StateFlow<List<Assistant>> =
        settingsStore.settingsFlow
            .map { settings -> settings.assistants.filter { it.name.isNotBlank() } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** 按 id 取角色，聊天页标题等场景用 */
    suspend fun getAssistant(id: Uuid): Assistant? =
        settingsStore.settingsFlow.first().assistants.firstOrNull { it.id == id }

    /**
     * 切换当前角色。
     *
     * 上游新建会话时读 settings.assistantId，所以「和某个角色开新聊天」必须先切到这里。
     */
    suspend fun selectAssistant(id: Uuid) = settingsStore.selectAssistant(id)
}

/**
 * 取最后一条消息的纯文本摘要，给列表用。
 *
 * 只取文字部分：图片、语音在列表里显示成占位符，
 * 和微信的「[图片]」「[语音]」体验一致。
 */
private fun Conversation.lastMessagePreview(): String {
    val node = messageNodes.lastOrNull() ?: return ""
    val message = node.messages.getOrNull(node.selectIndex) ?: return ""
    val text = message.parts
        .filterIsInstance<UIMessagePart.Text>()
        .joinToString(" ") { it.text }
        .replace(Regex("\\s+"), " ")
        .trim()
    return when {
        text.isNotEmpty() -> text.take(60)
        message.parts.any { it is UIMessagePart.Image } -> "[图片]"
        message.parts.any { it is UIMessagePart.Audio } -> "[语音]"
        else -> ""
    }
}
