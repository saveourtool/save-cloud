package org.cqfn.save.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(decoder.decodeLong()),
        ZoneOffset.UTC
    )

    override val descriptor = PrimitiveSerialDescriptor("timestamp", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeLong(value.toEpochSecond(ZoneOffset.UTC))
    }
}
