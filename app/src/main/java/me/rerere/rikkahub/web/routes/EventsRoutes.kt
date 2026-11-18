package me.rerere.rikkahub.web.routes

import io.ktor.server.routing.Route
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.data.repository.FolderRepository
import me.rerere.rikkahub.service.ChatService
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.rikkahub.web.dto.ConversationListInvalidateEvent
import me.rerere.rikkahub.web.dto.FolderListEvent
import me.rerere.rikkahub.web.dto.toDto
import kotlin.time.Duration.Companion.seconds
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.ProviderSecretCodec

/**
 * Multiplexed server-sent events stream.
 *
 * A single `/api/events` connection carries several event types, distinguished by the
 * SSE `event:` field, so new event kinds can be added without opening a new connection:
 *  - `settings`                     -> full Settings snapshot
 *  - `conversation_list_invalidate` -> the conversation list for an assistant changed
 *  - `folders`                      -> the folder list for an assistant changed
 *
 * Per-conversation streaming (generation updates) keeps its own dedicated connection
 * at `/api/conversations/{id}/stream`.
 */
fun Route.eventsRoutes(
    chatService: ChatService,
    conversationRepo: ConversationRepository,
    folderRepo: FolderRepository,
    settingsStore: SettingsStore,
) {
    sse("/events") {
        heartbeat {
            period = 15.seconds
        }

        // Full settings snapshot; StateFlow emits the current value immediately on connect.
        // API Key 必须在这里摘掉：这条流走的是内嵌 Web 服务器的 HTTP 接口，
        // 同一局域网里任何拿到地址的浏览器都能收到，明文 Key 等于公开。
        // 前端只需要知道有哪些服务商、选了哪个模型，不需要 Key 本身。
        val settingsEvents = settingsStore.settingsFlow.map { settings ->
            EventPayload(event = "settings", json = JsonInstant.encodeToString(settings.withoutSecrets()))
        }

        // Conversation list invalidation, scoped to the currently selected assistant.
        val conversationListEvents = settingsStore.settingsFlow
            .map { it.assistantId }
            .distinctUntilChanged()
            .flatMapLatest { assistantId ->
                combine(
                    conversationRepo.getConversationsOfAssistant(assistantId),
                    chatService.getConversationJobs()
                ) { conversations, generationJobs ->
                    // Key per conversation folds in generation state (so start/stop invalidates the
                    // sidebar even when content isn't persisted) and folderId (so moving a
                    // conversation between folders invalidates other clients' folder views).
                    conversations.map { conversation ->
                        buildString {
                            append(conversation.id)
                            append('|')
                            append(conversation.updateAt.toEpochMilli())
                            append('|')
                            append(generationJobs[conversation.id] != null)
                            append('|')
                            append(conversation.folderId?.toString().orEmpty())
                        }
                    }
                }
                    .distinctUntilChanged()
                    .map { assistantId }
            }
            .map { assistantId ->
                EventPayload(
                    event = "conversation_list_invalidate",
                    json = JsonInstant.encodeToString(
                        ConversationListInvalidateEvent(
                            assistantId = assistantId.toString(),
                            timestamp = System.currentTimeMillis()
                        )
                    )
                )
            }

        // Folder list for the currently selected assistant (Room flow emits on any change).
        val folderEvents = settingsStore.settingsFlow
            .map { it.assistantId }
            .distinctUntilChanged()
            .flatMapLatest { assistantId ->
                folderRepo.getFoldersOfAssistant(assistantId).map { folders ->
                    EventPayload(
                        event = "folders",
                        json = JsonInstant.encodeToString(
                            FolderListEvent(
                                assistantId = assistantId.toString(),
                                folders = folders.map { it.toDto() }
                            )
                        )
                    )
                }
            }

        merge(settingsEvents, conversationListEvents, folderEvents).collect { payload ->
            send(data = payload.json, event = payload.event)
        }
    }
}

private data class EventPayload(
    val event: String,
    val json: String,
)

/**
 * 去掉敏感字段后的 [Settings] 副本。
 *
 * 目前只处理 `providers[].apiKey`——设置里其它内容（模型选择、主题、
 * 助手列表）前端要用，也不算秘密，所以只清 Key。
 */
private fun Settings.withoutSecrets(): Settings =
    copy(providers = ProviderSecretCodec.stripAll(providers))
