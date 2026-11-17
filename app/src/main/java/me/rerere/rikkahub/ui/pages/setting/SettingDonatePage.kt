package me.rerere.rikkahub.ui.pages.setting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Github
import me.rerere.hugeicons.stroke.Earth
import me.rerere.hugeicons.stroke.Message01
import me.rerere.hugeicons.stroke.UserCircle
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.CardGroupIcon
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.openUrl

/** 作者名 */
private const val AUTHOR_NAME = "嘉酱"

/** 作者 QQ */
private const val AUTHOR_QQ = "2924574099"

/** 主仓库（这个 App 的源码与构建） */
private const val REPO_URL = "https://github.com/longjiajiang757/ai-wechat"

/** 上游仓库（界面与核心功能来自 RikkaHub） */
private const val UPSTREAM_URL = "https://github.com/rikkahub/rikkahub"

/** 下载页 */
private const val RELEASE_URL = "https://github.com/longjiajiang757/ai-wechat/releases/latest"

/** 直接下载 APK 的固定地址 */
private const val APK_URL =
    "https://github.com/longjiajiang757/ai-wechat/releases/download/latest/ai-wechat.apk"

@Composable
fun SettingDonatePage() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(text = stringResource(R.string.donate_page_title))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { paddings ->
        Column(
            modifier = Modifier
                .padding(paddings)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthorHeader()

            AuthorCardGroup(
                onCopyQq = {
                    copyToClipboard(context, AUTHOR_QQ)
                    Toast.makeText(
                        context,
                        context.getString(R.string.donate_page_qq_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            LinksCardGroup(
                onOpenRepo = { context.openUrl(REPO_URL) },
                onOpenUpstream = { context.openUrl(UPSTREAM_URL) },
                onOpenRelease = { context.openUrl(RELEASE_URL) },
            )

            Text(
                text = stringResource(R.string.donate_page_apk_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = APK_URL,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AuthorHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = AUTHOR_NAME,
                style = MaterialTheme.typography.headlineMediumEmphasized.copy(
                    fontWeight = FontWeight.Black,
                ),
            )
            Text(
                text = stringResource(R.string.donate_page_author_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AuthorCardGroup(onCopyQq: () -> Unit) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    CardGroup(
        modifier = Modifier.fillMaxWidth(),
        title = { Text(stringResource(R.string.donate_page_author_group)) },
    ) {
        item(
            leadingContent = {
                CardGroupIcon(
                    HugeIcons.UserCircle,
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            },
            supportingContent = { Text(stringResource(R.string.donate_page_author_name_desc)) },
            headlineContent = { Text(AUTHOR_NAME) },
        )
        item(
            onClick = onCopyQq,
            leadingContent = {
                CardGroupIcon(
                    HugeIcons.Message01,
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            },
            supportingContent = { Text(stringResource(R.string.donate_page_qq_desc)) },
            headlineContent = { Text("QQ $AUTHOR_QQ") },
        )
    }
}

@Composable
private fun LinksCardGroup(
    onOpenRepo: () -> Unit,
    onOpenUpstream: () -> Unit,
    onOpenRelease: () -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.tertiaryContainer
    val contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    CardGroup(
        modifier = Modifier.fillMaxWidth(),
        title = { Text(stringResource(R.string.donate_page_links_group)) },
    ) {
        item(
            onClick = onOpenRepo,
            leadingContent = {
                CardGroupIcon(
                    HugeIcons.Github,
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            },
            supportingContent = { Text(REPO_URL) },
            headlineContent = { Text(stringResource(R.string.donate_page_repo)) },
        )
        item(
            onClick = onOpenUpstream,
            leadingContent = {
                CardGroupIcon(
                    HugeIcons.Github,
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            },
            supportingContent = { Text(UPSTREAM_URL) },
            headlineContent = { Text(stringResource(R.string.donate_page_upstream)) },
        )
        item(
            onClick = onOpenRelease,
            leadingContent = {
                CardGroupIcon(
                    HugeIcons.Earth,
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            },
            supportingContent = { Text(stringResource(R.string.donate_page_download_desc)) },
            headlineContent = { Text(stringResource(R.string.donate_page_download)) },
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("qq", text))
}
