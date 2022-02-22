package org.cqfn.save.utils

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("timestamp", PrimitiveKind.LONG)
    private val kotlinxSerializer = kotlinx.datetime.LocalDateTime.serializer()

    @Suppress("MagicNumber")
    override fun deserialize(decoder: Decoder): LocalDateTime {
        return kotlinxSerializer.deserialize(decoder).toJavaLocalDateTime()
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        return kotlinxSerializer.serialize(encoder, value.toKotlinLocalDateTime())
    }
}
