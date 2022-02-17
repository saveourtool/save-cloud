package org.cqfn.save.utils

import java.time.LocalDateTime
import java.time.ZoneOffset

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("timestamp", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val dateFormat = decoder.decodeSerializableValue(serializer<List<Int>>())
        return LocalDateTime.of(dateFormat[0], dateFormat[1], dateFormat[2], dateFormat[3], dateFormat[4])
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeLong(value.toEpochSecond(ZoneOffset.UTC))
    }
}
