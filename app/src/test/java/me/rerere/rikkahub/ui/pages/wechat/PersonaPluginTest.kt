package me.rerere.rikkahub.ui.pages.wechat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 插件归属判定与插件表本身的测试。
 *
 * 装/卸载的完整流程依赖 SettingsStore，跑不了 JVM 测试；
 * 但「这条注入属于哪个插件」是所有状态判定的基础，出错会导致：
 * 装完显示未装、卸载卸载错、或者一条注入被两个插件同时认领。
 */
class PersonaPluginTest {

    @Test
    fun `插件 id 不重复`() {
        val ids = PERSONA_PLUGINS.map { it.id }
        assertEquals("插件 id 必须唯一，否则安装状态会串", ids.size, ids.distinct().size)
    }

    @Test
    fun `插件名不重复`() {
        val names = PERSONA_PLUGINS.map { it.name }
        assertEquals("插件名重复会让界面分不清", names.size, names.distinct().size)
    }

    @Test
    fun `描述文案非空`() {
        PERSONA_PLUGINS.forEach {
            assertTrue("插件 ${it.id} 缺描述", it.description.isNotBlank())
        }
    }

    @Test
    fun `提示词不重复且非空`() {
        val prompts = PERSONA_PLUGINS.map { it.prompt }
        assertTrue("提示词不能为空，否则装了等于没装", prompts.all { it.isNotBlank() })
        assertEquals(
            "提示词是识别插件归属的依据，重复会导致误判",
            prompts.size,
            prompts.distinct().size
        )
    }

    @Test
    fun `通过内容能找回插件`() {
        // 插件页就是靠这个把注入反查回插件
        PERSONA_PLUGINS.forEach { plugin ->
            val found = PERSONA_PLUGINS.firstOrNull { it.prompt == plugin.prompt }
            assertEquals(plugin.id, found?.id)
        }
    }

    @Test
    fun `非插件注入不会被误认`() {
        // 用户自己建的普通注入不该被当成插件
        val userInjection = "你是一个有用的助手。"
        assertNull(PERSONA_PLUGINS.firstOrNull { it.prompt == userInjection })
    }

    @Test
    fun `插件数量合理`() {
        // 太少显得空，太多列表没重点
        assertTrue("插件市场至少要有几个可选", PERSONA_PLUGINS.size >= 5)
        assertFalse("插件不该无限多", PERSONA_PLUGINS.size > 20)
    }
}