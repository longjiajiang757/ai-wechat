package me.rerere.rikkahub.data.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.utils.JsonInstant

/**
 * V6：
 * 1. 删掉旧版自带但从未被改过的「第二个默认助手」——界面上和第一个
 *    同名（都显示「默认助手」），新用户会以为重复了。改过名/人设的
 *    不动，那是用户自己的东西。
 * 2. 所有角色打开记忆（enableMemory）。这个 App 的核心体验是
 *    「她记得我」，之前默认关着，绝大多数人根本不知道有开关，
 *    聊了很多记忆星海却是空的。
 */
class PreferenceStoreV6Migration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[SettingsStore.VERSION]
        return version == null || version < 6
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()

        prefs[SettingsStore.ASSISTANTS]?.let { json ->
            prefs[SettingsStore.ASSISTANTS] = migrateAssistantsV6(json)
        }

        prefs[SettingsStore.VERSION] = 6
        return prefs.toPreferences()
    }

    override suspend fun cleanUp() {}
}

/** 备份文件里的 settings.json 走同一套逻辑 */
fun migrateAssistantsJsonV6(assistantsJson: String): String = migrateAssistantsV6(assistantsJson)

private const val SECOND_DEFAULT_ASSISTANT_ID = "3d47790c-c415-4b90-9388-751128adb0a0"

/**
 * 对 assistants JSON 数组字符串应用 V6 规则。
 * 任何解析问题都原样返回——迁移失败不能把用户设置弄丢。
 */
internal fun migrateAssistantsV6(assistantsJson: String): String {
    return runCatching {
        val root = JsonInstant.parseToJsonElement(assistantsJson) as? JsonArray
            ?: return@runCatching assistantsJson

        val filtered = root.filterIsInstance<JsonObject>().filterNot { assistant ->
            // 只删「还是出厂样子」的第二个默认助手：id 命中且没人动过它
            assistant["id"]?.jsonPrimitive?.contentOrNull == SECOND_DEFAULT_ASSISTANT_ID &&
                (assistant["name"]?.jsonPrimitive?.contentOrNull.isNullOrBlank())
        }

        val migrated = JsonArray(filtered.map { assistant ->
            val memoryOn = assistant["enableMemory"]?.jsonPrimitive?.booleanOrNull == true
            if (memoryOn) assistant
            else JsonObject(assistant.toMutableMap().apply { put("enableMemory", JsonTrue) })
        })

        JsonInstant.encodeToString(migrated)
    }.getOrDefault(assistantsJson)
}

private val JsonTrue = kotlinx.serialization.json.JsonPrimitive(true)