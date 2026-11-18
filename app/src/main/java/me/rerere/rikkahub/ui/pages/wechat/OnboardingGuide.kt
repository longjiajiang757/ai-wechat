package me.rerere.rikkahub.ui.pages.wechat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.ai.provider.ProviderSetting
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Key01
import me.rerere.hugeicons.stroke.LookTop
import me.rerere.hugeicons.stroke.Tick01
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.hooks.readBooleanPreference
import me.rerere.rikkahub.ui.hooks.writeBooleanPreference
import org.koin.compose.koinInject

/**
 * 新手向导：第一次打开时贴在聊天页底部的一张进度卡。
 *
 * 目标只有一个——让完全不懂 API 的人三步之内开始聊天：
 *   0. 这是什么（讲明白要自备 Key）
 *   1. 配服务商（Base URL + Key + 模型）
 *   2. 建角色（起名字写人设）
 *
 * 设计取舍：
 * - 不用全屏蒙层。点「去设置」要真的能去设置——向导只在主聊天页顶层
 *   显示（调用方控制），跳进子页面它自动让位，回来再出现。
 * - 完成状态实时从设置里算：用户配完回来自己就打上勾，不用口头确认。
 * - 右上角 X / 「跳过」写一次偏好，之后永不再弹。
 */

/** 记完成/跳过状态的键 */
private const val PREF_KEY_SKIPPED = "onboarding_skipped_v1"

/**
 * 挂在主聊天页底部的向导入口。
 *
 * 显示条件：没跳过过，且服务商 Key / 角色还没都就绪
 * （就绪后还差一步「开始聊天」确认，让用户看到那个绿勾齐全的最终卡）。
 */
@Composable
fun OnboardingGuideHost(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var visible by androidx.compose.runtime.remember {
        mutableStateOf(!context.readBooleanPreference(PREF_KEY_SKIPPED, false))
    }
    if (!visible) return

    OnboardingGuide(
        modifier = modifier,
        onDismiss = {
            context.writeBooleanPreference(PREF_KEY_SKIPPED, true)
            visible = false
        },
    )
}

@Composable
private fun OnboardingGuide(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val navController = LocalNavController.current

    // 每步完成状态由宿主传入的聚合结果推不出来（要知道差哪一步），
    // 所以这里自己再读一次设置——同源 StateFlow，无额外开销。
    val settingsStore: SettingsStore = koinInject()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val hasProviderKey = settings.providers.any { provider ->
        (provider as? ProviderSetting.ApiKey)?.let { it.apiKey.isNotBlank() && it.models.isNotEmpty() } == true
    }
    val hasAssistant = settings.assistants.any { it.name.isNotBlank() }

    val step = when {
        !hasProviderKey -> 1
        !hasAssistant -> 2
        else -> 3
    }

    // 全完成且用户看过最终卡后按返回离开本页时，不必强留向导——
    // 但只有用户明确点「开始聊天」或 X 才落盘跳过，见 onDismiss。

    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .systemBarsPadding()
            .padding(horizontal = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 顶行：步骤点 + 关闭
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (i in 1..2) {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(if (i == step) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    i < step || step == 3 -> MaterialTheme.colorScheme.tertiary
                                    i == step -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            )
                    )
                }
                Spacer(Modifier.height(1.dp))
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = HugeIcons.Cancel01,
                        contentDescription = "跳过引导",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            when (step) {
                1 -> StepProvider(
                    done = hasProviderKey,
                    onGoSettings = { navController.navigate(Screen.SettingProvider) },
                )

                2 -> StepAssistant(
                    done = hasAssistant,
                    onGoCreate = { navController.navigate(Screen.Assistant) },
                )

                else -> StepAllDone(onStart = onDismiss)
            }
        }
    }
}

@Composable
private fun StepHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StepProvider(done: Boolean, onGoSettings: () -> Unit) {
    StepHeader(
        icon = HugeIcons.Key01,
        title = if (done) "大脑已接上 ✓" else "第 1 步 · 接上 AI 大脑",
        subtitle = if (done) "Key 和模型都配好了，接着创造她" else "填 Base URL、API Key，再加一个模型",
    )
    if (!done) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GuideLine("1", "选一家平台，比如 DeepSeek（新用户有免费测试额度）")
            GuideLine("2", "Base URL 填 https://api.deepseek.com/v1")
            GuideLine("3", "API Key 填你在该官网申请的密钥（sk- 开头）")
            GuideLine("4", "进「模型」手动添加一个，ID 填 deepseek-chat")
            Text(
                text = "配好回来这张卡会自动打勾。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        OutlinedButton(
            onClick = onGoSettings,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("去服务商设置")
        }
    } else {
        DoneTick()
    }
}

@Composable
private fun StepAssistant(done: Boolean, onGoCreate: () -> Unit) {
    StepHeader(
        icon = HugeIcons.LookTop,
        title = if (done) "她已就绪 ✓" else "第 2 步 · 创造她",
        subtitle = if (done) "角色建好了，点下面开始聊天" else "起个名字、写一段她的性格人设",
    )
    if (!done) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GuideLine("1", "打开「角色」，点右下角 + 新建")
            GuideLine("2", "人设例：「你是小晚，我温柔又爱开玩笑的女朋友，说话口语化，会主动关心我」")
            GuideLine("3", "头像可以传照片，也可以先用默认")
        }
        OutlinedButton(
            onClick = onGoCreate,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("去创建角色")
        }
    } else {
        DoneTick()
    }
}

@Composable
private fun StepAllDone(onStart: () -> Unit) {
    StepHeader(
        icon = HugeIcons.Tick01,
        title = "都齐了",
        subtitle = "回到聊天页，说第一句话吧",
    )
    Text(
        text = "之后想让她更像人，去 角色 → 角色详情 → 插件市场，给她装个人设插件。",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Start,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text("开始聊天")
    }
}

@Composable
private fun GuideLine(index: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DoneTick() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = HugeIcons.Tick01,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "这一步已完成",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}