package me.rerere.rikkahub.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.rerere.common.http.await
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 公告服务。
 *
 * 架构是纯客户端（BYOK，没有自己的后端），所以「推送」实际是
 * 每次冷启动拉一次仓库里托管的公告 JSON：
 *   https://raw.githubusercontent.com/longjiajiang757/ai-wechat/main/announcements.json
 * 发公告 = 往仓库改这个文件推上去，全世界几分钟内生效，不用发版。
 *
 * 去重：App 把读过的公告 id 存 SharedPreferences，
 * 同一条只在冷启动弹一次；设置页「官方公告」可随时重看。
 *
 * 失败静默：断网、GitHub 被墙、JSON 改坏，都返回 null 什么也不弹——
 * 公告坏了不能影响聊天主功能。
 */
private const val ANNOUNCEMENT_URL =
    "https://raw.githubusercontent.com/longjiajiang757/ai-wechat/main/announcements.json"

@Serializable
data class Announcement(
    /** 每条公告的唯一 id（换 id 才会重新弹窗），如 "2026-11-19-beta" */
    val id: String,
    val title: String,
    val body: String,
    @SerialName("publishedAt") val publishedAt: String = "",
    /** 用户能否关闭；false 时只提供滚动确认（暂不用） */
    val dismissable: Boolean = true,
)

class AnnouncementChecker(
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** 拉取公告；任何异常返回 null */
    suspend fun fetch(): Announcement? = runCatching {
        val response = client.newCall(
            Request.Builder()
                .url(ANNOUNCEMENT_URL)
                .get()
                .build()
        ).await()
        if (!response.isSuccessful) return@runCatching null
        json.decodeFromString<Announcement>(response.body.string())
    }.getOrNull()
}