package com.saveourtool.save.demo.cpg.entity

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
 * Print only [DemoEntity.value]
 */
class DemoEntitySerializer : StdSerializer<DemoEntity>(DemoEntity::class.java) {
    override fun serialize(valueNullable: DemoEntity?, gen: JsonGenerator?, provider: SerializerProvider?) {
        valueNullable?.let { value ->
            requireNotNull(gen).writeString(value.value)
        }
    }
}
