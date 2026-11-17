package me.rerere.rikkahub.data.datastore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ProviderSecretCodec] 的判定逻辑测试。
 *
 * 只测「要不要加密/解密」这两个纯判断：
 * 真正的加解密走 Android Keystore，JVM 单元测试里跑不了（会抛 NoClassDefFoundError），
 * 那部分只能靠真机验证。但这里覆盖的正是最容易出错的地方：
 * 重复加密套娃、老数据兼容、空值处理。
 */
class ProviderSecretCodecTest {

    // ---------- needsEncrypt ----------

    @Test
    fun `普通明文需要加密`() {
        assertTrue(ProviderSecretCodec.needsEncrypt("sk-abc123"))
    }

    @Test
    fun `空字符串不加密`() {
        // 空 key 很常见（用户还没填），没必要为了它去碰 Keystore
        assertFalse(ProviderSecretCodec.needsEncrypt(""))
    }

    @Test
    fun `已加密的值不重复加密`() {
        // 这条最关键：否则每次读写都会多包一层，很快就解不开了
        assertFalse(ProviderSecretCodec.needsEncrypt("v1:YWJjZGVm"))
    }

    // ---------- needsDecrypt ----------

    @Test
    fun `密文需要解密`() {
        assertTrue(ProviderSecretCodec.needsDecrypt("v1:YWJjZGVm"))
    }

    @Test
    fun `升级前的明文原样放行`() {
        // 老用户的 settings 里是明文 key，加了加密后必须还能读出来
        assertFalse(ProviderSecretCodec.needsDecrypt("sk-abc123"))
    }

    @Test
    fun `空字符串不解密`() {
        assertFalse(ProviderSecretCodec.needsDecrypt(""))
    }

    // ---------- 往返一致性 ----------

    @Test
    fun `前缀判定互斥`() {
        // 任何一个非空值，要么该加密要么该解密，不能两个都真或都假
        listOf("sk-abc", "v1:YWJj", "plain-key-123").forEach { value ->
            val enc = ProviderSecretCodec.needsEncrypt(value)
            val dec = ProviderSecretCodec.needsDecrypt(value)
            assertTrue("值 [$value] 不能同时需要加密和解密", !(enc && dec))
            assertTrue("值 [$value] 必须落在加密或解密其中之一", enc || dec)
        }
    }
}