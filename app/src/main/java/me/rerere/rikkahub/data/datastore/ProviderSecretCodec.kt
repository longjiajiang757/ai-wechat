package me.rerere.rikkahub.data.datastore

import android.util.Log
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.utils.SecretCipher

/**
 * 服务商 API Key 的落盘加密。
 *
 * 背景：`Settings` 里的 `providers` 是一个整体 JSON 存进 DataStore 的，
 * 里面每个服务商的 `apiKey` 都是明文。手机 root、备份导出、或者别的 App
 * 读到 DataStore 文件，key 就泄露了。所以写盘前加密、读盘后解密。
 *
 * 设计要点：
 * 1. 只加密 `apiKey` 这一个字段，其它字段照旧明文——它们不敏感，
 *    而且加密后没法用肉眼排查配置问题。
 * 2. 密文带 `v1:` 前缀（[SecretCipher] 自己的格式）。解密时靠前缀判断，
 *    这样老用户的明文 key 还能正常读出来，不需要迁移脚本。
 * 3. 解密失败（换机、重装、系统重置导致 Keystore 密钥没了）时返回空字符串，
 *    让用户重新填，而不是让整个 App 崩在读取设置这一步。
 * 4. [SecretCipher] 依赖 Android Keystore，只在真机/模拟器上能用；
 *    这里捕获所有异常，保证在任何环境下序列化都不会失败。
 */
internal object ProviderSecretCodec {

    private const val TAG = "ProviderSecretCodec"

    /** [SecretCipher] 的密文前缀，与 SecretCipher.VERSION_PREFIX 保持一致 */
    private const val SECRET_PREFIX = "v1:"

    /**
     * 写盘前：把所有服务商的 `apiKey` 换成密文。
     *
     * 已经是密文的不重复加密，避免多次读写后套娃。
     */
    fun encryptAll(providers: List<ProviderSetting>): List<ProviderSetting> =
        providers.map { provider -> mapApiKey(provider) { plain -> encryptOne(plain) } }

    /**
     * 读盘后：把密文还原成明文。
     *
     * 明文（老数据或用户手动改过）原样返回，保证向后兼容。
     */
    fun decryptAll(providers: List<ProviderSetting>): List<ProviderSetting> =
        providers.map { provider -> mapApiKey(provider) { stored -> decryptOne(stored) } }

    /**
     * 摘掉所有 API Key，用于把设置发给 App 之外的地方。
     *
     * 内嵌 Web 服务器（`/api/events`）会把 Settings 整个推给浏览器，
     * 同一局域网里能访问到地址的人都能收到。那里只需要知道有哪些服务商、
     * 选了哪个模型，不需要 Key，所以发送前统一清空。
     *
     * 与加密不同：这里不是「换个形式存」，而是真的不给。
     */
    fun stripAll(providers: List<ProviderSetting>): List<ProviderSetting> =
        providers.map { provider -> mapApiKey(provider) { "" } }

    private fun encryptOne(plain: String): String {
        if (!needsEncrypt(plain)) return plain
        return runCatching { SecretCipher.encrypt(plain) }
            .getOrElse {
                Log.w(TAG, "encrypt failed, keeping plaintext", it)
                plain
            }
    }

    private fun decryptOne(stored: String): String {
        if (!needsDecrypt(stored)) return stored
        return runCatching { SecretCipher.decrypt(stored) }
            .getOrElse {
                // Keystore 密钥丢失（换机/重装）：清空让用户重填，不能让 App 崩
                Log.w(TAG, "decrypt failed, clearing api key", it)
                ""
            }
    }

    /**
     * 这个值要不要加密。
     *
     * 空值不用管；已经带前缀的说明加密过了，再加密会套娃。
     */
    internal fun needsEncrypt(value: String): Boolean =
        value.isNotEmpty() && !value.startsWith(SECRET_PREFIX)

    /**
     * 这个值要不要解密。
     *
     * 只有带前缀的才是密文；升级前的老数据是明文，要原样放行。
     */
    internal fun needsDecrypt(value: String): Boolean =
        value.isNotEmpty() && value.startsWith(SECRET_PREFIX)

    /**
     * 对单个服务商做 `apiKey` 变换。
     *
     * `ApiKey` 子类里 `apiKey` 是 `var`，可以直接赋值；
     * 账号型（`Account`）没有 apiKey，原样返回。
     */
    private inline fun mapApiKey(
        provider: ProviderSetting,
        transform: (String) -> String,
    ): ProviderSetting {
        if (provider !is ProviderSetting.ApiKey) return provider
        val old = provider.apiKey
        val new = transform(old)
        if (new == old) return provider
        // apiKey 是 var，但 sealed 层级下无法直接写，用 copyProvider 复制再赋值
        return provider.copyProvider().also { copy ->
            (copy as? ProviderSetting.ApiKey)?.let { writable ->
                // 子类实现是 var，可赋值
                assignApiKey(writable, new)
            }
        }
    }
}

/**
 * 把 [apiKey] 写回具体的服务商实例。
 *
 * 三个 ApiKey 子类的 apiKey 都是 `var`，但抽象类型上是 `val`，
 * Kotlin 不允许通过父类型给 val 赋值，所以按类型分派。
 * 这样写比反射安全，编译期就能发现漏掉的类型。
 */
private fun assignApiKey(provider: ProviderSetting.ApiKey, value: String) {
    when (provider) {
        is ProviderSetting.OpenAI -> provider.apiKey = value
        is ProviderSetting.Google -> provider.apiKey = value
        is ProviderSetting.Claude -> provider.apiKey = value
        else -> Unit
    }
}