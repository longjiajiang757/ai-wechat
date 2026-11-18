package me.rerere.rikkahub.ui.pages.wechat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.Brain02
import me.rerere.hugeicons.stroke.Sparkles
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.model.MemoryFile
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantDetailVM
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 记忆星海。
 *
 * 上游的 AssistantMemoryPage 是「文件管理器」视角：按目录分组、带定稿状态、分页切换，
 * 那是给人管理记忆用的。但用户想看到的是「她记住了我什么」——这是一种情感视角，
 * 所以这里换成一个星星网格：每条记忆是一颗星，点开能看全文。
 *
 * 数据仍来自上游 AssistantDetailVM，不重复实现读取逻辑。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeChatMemoryPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(parameters = { parametersOf(id) })
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val memories by vm.memories.collectAsStateWithLifecycle()
    val navController = LocalNavController.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("记忆星海", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${assistant.name} 记住了 ${memories.size} 件事",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = { BackButton() },
                actions = {
                    Icon(
                        imageVector = HugeIcons.Add01,
                        contentDescription = "新建记忆",
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(22.dp)
                            .clickable {
                                navController.navigate(Screen.MemoryFile(vm.memoryId, null))
                            }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        }
    ) { paddings ->
        if (memories.isEmpty()) {
            EmptyMemoryHint(
                modifier = Modifier
                    .padding(paddings)
                    .fillMaxSize()
            )
            return@Scaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(paddings)
                .fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(memories, key = { it.path }) { memory ->
                MemoryStarCard(
                    memory = memory,
                    onClick = {
                        navController.navigate(Screen.MemoryFile(vm.memoryId, memory.path))
                    }
                )
            }
        }
    }
}

/**
 * 一颗「星」。
 *
 * 用色块而不是图片，避免为每条记忆额外生成资源；
 * 颜色按路径哈希取，同一条记忆每次都长一样，有辨识度。
 */
@Composable
private fun MemoryStarCard(
    memory: MemoryFile,
    onClick: () -> Unit,
) {
    val tint = memoryTint(memory.path)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = HugeIcons.Sparkles,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = tint
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = memory.memoryTitle(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = memory.previewText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyMemoryHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = HugeIcons.Brain02,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "星海还是空的",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "多聊聊，她会慢慢记住你",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 记忆标题：优先用 frontmatter 里的名字，没有就用文件名 */
private fun MemoryFile.memoryTitle(): String {
    val fromName = path.substringAfterLast('/').substringBeforeLast('.')
    return fromName.ifBlank { "记忆" }
}

/** 卡片上显示的摘要：去掉 frontmatter 的正文前几十个字 */
private fun MemoryFile.previewText(): String =
    body.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .joinToString(" ")
        .take(80)
        .ifBlank { description.ifBlank { "（空）" } }

/** 按路径稳定取色，让每条记忆有自己的颜色 */
private fun memoryTint(path: String): Color {
    val palette = listOf(
        Color(0xFF07C160), // 微信绿
        Color(0xFF3A7AFE), // 蓝
        Color(0xFFFA9D3B), // 橙
        Color(0xFF9B59D0), // 紫
        Color(0xFF00B4A6), // 青
    )
    // hashCode 可能为负，用 floorMod 保证落在合法下标内
    val index = Math.floorMod(path.hashCode(), palette.size)
    return palette[index]
}