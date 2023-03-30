package com.saveourtool.save.demo.cpg.entity

import org.neo4j.ogm.typeconversion.CompositeAttributeConverter

/**
 * Location of node in tree-sitter
 */
class TreeSitterLocation {
    /**
     * file name of location
     */
    var fileName: String = "N/A"

    /**
     * start in bytes of location
     */
    var startBytes: Int = 0

    /**
     * end in bytes of location
     */
    var endBytes: Int = 0

    override fun toString(): String = "TreeSitterLocation(fileName='$fileName', startBytes=$startBytes, endBytes=$endBytes)"

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

            override fun toEntityAttribute(value: Map<String?, *>): TreeSitterLocation? = try {
                TreeSitterLocation().apply {
                    fileName = value.getValue(FILE_NAME).toString()
                    startBytes = value.getValue(START_BYTES).toString().toInt()
                    endBytes = value.getValue(END_BYTES).toString().toInt()
                }
            } catch (e: NullPointerException) {
                null
            }

            companion object {
                const val END_BYTES = "endBytes"
                const val FILE_NAME = "file"
                const val LOCATION = "location"
                const val START_BYTES = "startBytes"
            }
        }
    }
}
