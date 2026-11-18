package me.rerere.rikkahub.utils

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.rerere.common.http.await
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request

// 更新源指向我们自己的仓库（固定 tag "latest"，每次 CI 构建后覆盖发布）。
// 不用上游 rikka-ai.com：那边推的是 RikkaHub 官方版，用户装了会误以为有新版。
private const val REPO_API = "https://api.github.com/repos/longjiajiang757/ai-wechat/releases/tags/latest"

class UpdateChecker(
    private val client: OkHttpClient,
    appScope: AppScope,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val updateState: StateFlow<UiState<UpdateInfo>> = checkUpdate().stateIn(
        scope = appScope,
        started = SharingStarted.Lazily,
        initialValue = UiState.Loading,
    )

    private fun checkUpdate(): Flow<UiState<UpdateInfo>> = flow {
        emit(UiState.Loading)
        emit(
            UiState.Success(
                data = try {
                    val response = client.newCall(
                        Request.Builder()
                            .url(REPO_API)
                            .get()
                            .addHeader(
                                "User-Agent",
                                "AI-agent ${BuildConfig.VERSION_NAME} #${BuildConfig.VERSION_CODE}"
                            )
                            .build()
                    ).await()
                    if (!response.isSuccessful) {
                        throw Exception("Failed to fetch update info")
                    }
                    // GitHub 的 release JSON 结构和上游更新服务的格式不一样，
                    // 在这里转一次，UI 层不用动。
                    val release = json.decodeFromString<GithubRelease>(response.body.string())
                    UpdateInfo(
                        // tag 固定是 "latest" 没有版本信息，真实版本号是 CI 写进 name 的。
                        // name 可能是 "1.0 Beta" 这种带文案的，只取数字段比较，
                        // 展示用原文。
                        version = release.name.removePrefix("v").trim(),
                        publishedAt = release.publishedAt,
                        changelog = release.body,
                        downloads = release.assets.map { asset ->
                            UpdateDownload(
                                name = asset.name,
                                url = asset.browserDownloadUrl,
                                size = formatSize(asset.size),
                            )
                        },
                    )
                } catch (e: Exception) {
                    throw Exception("Failed to fetch update info", e)
                }
            )
        )
    }.catch {
        emit(UiState.Error(it))
    }.flowOn(Dispatchers.IO)

    fun downloadUpdate(context: Context, download: UpdateDownload) {
        runCatching {
            val request = DownloadManager.Request(download.url.toUri()).apply {
                // 设置下载时通知栏的标题和描述
                setTitle(download.name)
                setDescription("正在下载更新包...")
                // 下载完成后通知栏可见
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                // 允许在移动网络和WiFi下下载
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                // 设置文件保存路径
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, download.name)
                // 允许下载的文件类型
                setMimeType("application/vnd.android.package-archive")
            }
            // 获取系统的DownloadManager
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            // 你可以保存返回的downloadId到本地，以便后续查询下载进度或状态
        }.onFailure {
            Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
            context.openUrl(download.url) // 跳转到下载页面
        }
    }
}

@Serializable
data class UpdateDownload(
    val name: String,
    val url: String,
    val size: String
)

@Serializable
data class UpdateInfo(
    val version: String,
    val publishedAt: String,
    val changelog: String,
    val downloads: List<UpdateDownload>
)

/**
 * 版本号值类，封装版本号字符串并提供比较功能
 *
 * 支持完整的 SemVer 规范：MAJOR.MINOR.PATCH[-prerelease][+build]
 * - 预发布版本优先级低于正式版：1.0.0-alpha < 1.0.0
 * - 预发布标识符按段逐个比较：数字按数值比较，字符串按字典序比较
 * - 预发布标识符优先级：alpha < beta < rc（通过字典序自然满足）
 * - build metadata（+号后面的部分）不影响优先级比较
 */
@JvmInline
value class Version(val value: String) : Comparable<Version> {

    private fun parse(): ParsedVersion {
        // 去掉 build metadata（+号后面的部分）
        val withoutBuild = value.split("+").first()
        // 分离主版本号和预发布标识符
        val hyphenIndex = withoutBuild.indexOf('-')
        val (coreStr, prereleaseStr) = if (hyphenIndex >= 0) {
            withoutBuild.substring(0, hyphenIndex) to withoutBuild.substring(hyphenIndex + 1)
        } else {
            withoutBuild to null
        }
        val core = coreStr.split(".").map { it.toIntOrNull() ?: 0 }
        val prerelease = prereleaseStr?.split(".")
        return ParsedVersion(core, prerelease)
    }

    override fun compareTo(other: Version): Int {
        val a = this.parse()
        val b = other.parse()

        // 先比较主版本号
        val maxLen = maxOf(a.core.size, b.core.size)
        for (i in 0 until maxLen) {
            val ap = if (i < a.core.size) a.core[i] else 0
            val bp = if (i < b.core.size) b.core[i] else 0
            if (ap != bp) return ap.compareTo(bp)
        }

        // 主版本号相同时比较预发布标识符
        // 有预发布标识符的版本优先级低于没有的：1.0.0-alpha < 1.0.0
        return when {
            a.prerelease == null && b.prerelease == null -> 0
            a.prerelease != null && b.prerelease == null -> -1
            a.prerelease == null && b.prerelease != null -> 1
            else -> comparePrerelease(a.prerelease!!, b.prerelease!!)
        }
    }

    companion object {
        fun compare(version1: String, version2: String): Int {
            return Version(version1).compareTo(Version(version2))
        }

        private fun comparePrerelease(a: List<String>, b: List<String>): Int {
            val maxLen = maxOf(a.size, b.size)
            for (i in 0 until maxLen) {
                // 字段少的优先级更低：1.0.0-alpha < 1.0.0-alpha.1
                if (i >= a.size) return -1
                if (i >= b.size) return 1

                val aNum = a[i].toIntOrNull()
                val bNum = b[i].toIntOrNull()

                val cmp = when {
                    // 都是字：按数值比较
                    aNum != null && bNum != null -> aNum.compareTo(bNum)
                    // 数字优先级低于字符串
                    aNum != null -> -1
                    bNum != null -> 1
                    // 都是字符串：按字典序比较
                    else -> a[i].compareTo(b[i])
                }
                if (cmp != 0) return cmp
            }
            return 0
        }
    }
}

private data class ParsedVersion(
    val core: List<Int>,
    val prerelease: List<String>?,
)

// 扩展操作符函数，使比较更直观
operator fun String.compareTo(other: Version): Int = Version(this).compareTo(other)
operator fun Version.compareTo(other: String): Int = this.compareTo(Version(other))

/**
 * GitHub release API 里我们关心的字段。
 *
 * 只声明用到的，ignoreUnknownKeys 兜住其余几十个字段。
 * tag 是 "latest" 固定值，真实版本从 name 里拿
 * （CI 发布时 name 写成版本号，比如 "1.1 Beta"）。
 */
@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String,
    @SerialName("published_at") val publishedAt: String,
    val body: String = "",
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
private data class GithubAsset(
    val name: String,
    val size: Long,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)

/** 把字节数变成人话：18.9 MB */
private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> String.format("%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format("%.0f KB", bytes / 1_000.0)
    else -> "$bytes B"
}
