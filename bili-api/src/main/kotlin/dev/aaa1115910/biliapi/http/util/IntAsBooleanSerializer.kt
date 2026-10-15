package dev.aaa1115910.biliapi.http.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 用于处理 API 返回 Int(0/1) 但代码期望 Boolean 的情况。
 *
 * B 站的动态流、用户卡片等接口会把 `following` 之类的布尔字段返回成 `0`/`1`，
 * 直接用 `Boolean` 反序列化会抛异常，进而导致整页数据（动态列表、视频详情）解析失败。
 */
object IntAsBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IntAsBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        // 先按 Int 读，0 为 false、非 0 为 true；读不到 Int 时再退回到 Boolean
        return try {
            decoder.decodeInt() != 0
        } catch (e: Exception) {
            decoder.decodeBoolean()
        }
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeInt(if (value) 1 else 0)
    }
}
