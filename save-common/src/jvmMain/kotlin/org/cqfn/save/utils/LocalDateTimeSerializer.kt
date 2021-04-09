package org.cqfn.save.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("timestamp", PrimitiveKind.LONG)
    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(decoder.decodeLong()),
        ZoneOffset.UTC
    )

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeLong(value.toEpochSecond(ZoneOffset.UTC))
    }
}
