package me.rerere.rikkahub.ui.pages.wechat

import androidx.compose.ui.graphics.Color

/**
 * 微信风格配色。
 *
 * 只在这里定义颜色，界面文件引用常量，避免各处硬编码色值导致风格不统一。
 */
object WeChatColors {
    /** 微信主绿，用于发送按钮、自己发的气泡、选中态 */
    val Green = Color(0xFF07C160)

    /** 深一点的绿，用于按下反馈 */
    val GreenPressed = Color(0xFF06AD56)

    /** 聊天页背景灰 */
    val ChatBackground = Color(0xFFEDEDED)

    /** 接收消息气泡（白色） */
    val BubbleIncoming = Color(0xFFFFFFFF)

    /** 发送消息气泡（微信绿） */
    val BubbleOutgoing = Color(0xFF95EC69)

    /** 列表页背景（白） */
    val ListBackground = Color(0xFFFFFFFF)

    /** 分割线 */
    val Divider = Color(0xFFE5E5E5)

    /** 次要文字（时间、摘要） */
    val TextSecondary = Color(0xFF999999)

    /** 主要文字 */
    val TextPrimary = Color(0xFF191919)

    /** 底部 tab 未选中 */
    val TabInactive = Color(0xFF7A7E83)

    /** 底部 tab 选中 */
    val TabActive = Color(0xFF07C160)
}
