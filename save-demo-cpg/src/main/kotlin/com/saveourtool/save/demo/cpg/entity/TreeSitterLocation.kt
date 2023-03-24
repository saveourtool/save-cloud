package com.saveourtool.save.demo.cpg.entity

import org.neo4j.ogm.typeconversion.CompositeAttributeConverter

/**
 * Location of node in tree-sitter
 *
 * @property fileName
 * @property startBytes
 * @property endBytes
 */
data class TreeSitterLocation(
    var fileName: String,
    var startBytes: Int,
    var endBytes: Int,
) {
    companion object {

        /**
         * A converter for [TreeSitterLocation]
         */
        class Converter : CompositeAttributeConverter<TreeSitterLocation> {
            override fun toGraphProperties(value: TreeSitterLocation?): Map<String, Any> = value?.let {
                mapOf(
                    FILE_NAME to value.fileName,
                    START_BYTES to value.startBytes,
                    END_BYTES to value.endBytes,
                    LOCATION to value.toString(),
                )
            } ?: emptyMap()

            override fun toEntityAttribute(value: Map<String?, *>): TreeSitterLocation? {
                return try {
                    TreeSitterLocation(
                        value.getValue(FILE_NAME).toString(),
                        value.getValue(START_BYTES).toString().toInt(),
                        value.getValue(END_BYTES).toString().toInt(),
                    )
                } catch (e: NullPointerException) {
                    null
                }
            }

            companion object {
                const val START_BYTES = "startBytes"
                const val END_BYTES = "endBytes"
                const val FILE_NAME = "file"
                const val LOCATION = "location"
            }
        }
    }
}
