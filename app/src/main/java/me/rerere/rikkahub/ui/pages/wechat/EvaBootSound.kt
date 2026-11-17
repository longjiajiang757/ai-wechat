package me.rerere.rikkahub.ui.pages.wechat

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import me.rerere.rikkahub.R
import java.io.File

/**
 * 启动音效播放器。
 *
 * 声音来源有两个，按优先级：
 * 1. 用户自己放进 App 私有目录的 `boot_sound.*`（设置页可导入）
 *    这样用户可以放任何自己想要的声音，包括原版动画音效——
 *    文件由用户自己提供，App 只是播放器。
 * 2. 内置的 `eva_boot.ogg`，是合成音，不含任何第三方素材。
 *
 * 用 MediaPlayer 而不是 SoundPool：用户导入的可能是 mp3/m4a/flac，
 * MediaPlayer 支持的格式多，而且启动音只放一次，不需要 SoundPool 的低延迟。
 */
@Composable
fun rememberBootSoundPlayer(): BootSoundPlayer {
    val context = LocalContext.current
    val player = remember(context) { BootSoundPlayer(context) }

    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return player
}

class BootSoundPlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    /** 用户自己放的音效文件，存在就用它 */
    private fun customSoundFile(): File? {
        val dir = File(context.filesDir, "boot_sound")
        if (!dir.isDirectory) return null
        return dir.listFiles()
            ?.filter { it.isFile && it.length() > 1024 }
            ?.sortedByDescending { it.lastModified() }
            ?.firstOrNull()
    }

    fun play() {
        if (isMediaMuted(context)) return
        runCatching {
            release()
            val mp = MediaPlayer()
            val custom = customSoundFile()
            if (custom != null) {
                mp.setDataSource(custom.absolutePath)
            } else {
                val afd = context.resources.openRawResourceFd(R.raw.eva_boot) ?: return
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            }
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { it.release() }
            mp.prepareAsync()
            player = mp
        }.onFailure {
            release()
        }
    }

    fun release() {
        runCatching {
            player?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        player = null
    }
}

/** 系统媒体音量为 0 时不放启动音，避免用户在图书馆里被吓到 */
fun isMediaMuted(context: Context): Boolean {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
    return am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
}

/** 用户是否已经放了自定义音效 */
fun hasCustomBootSound(context: Context): Boolean {
    val dir = File(context.filesDir, "boot_sound")
    return dir.isDirectory && dir.listFiles()?.any { it.isFile && it.length() > 1024 } == true
}