package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

/**
 * @property name name of file
 * @property uploadedMillis version of file
 */
@Serializable
data class FileKey(
    val name: String,
    val uploadedMillis: Long,
) {
    /**
     * @param fileInfo
     */
    constructor(fileInfo: FileInfo) : this(fileInfo.name, fileInfo.uploadedMillis)

    /**
     * @return formatted string
     */
    fun format(): String = "$name$FIELD_DELIMITER$uploadedMillis"

    companion object {
        const val FIELD_DELIMITER = ":"
        const val OBJECT_DELIMITER = ";"

        /**
         * @param str
         * @return list of [FileKey]s parsed from provided string
         */
        fun parseList(str: String): List<FileKey> = if (str.isEmpty()) emptyList() else str.split(OBJECT_DELIMITER).map { parse(it) }

        /**
         * @param str
         * @return [FileKey] parsed from provided string
         */
        private fun parse(str: String): FileKey {
            val (name, uploadedMillis) = str.split(FIELD_DELIMITER)
            return FileKey(name, uploadedMillis.toLong())
        }
    }
}

/**
 * @return formatted string
 */
fun List<FileKey>.format(): String = this.joinToString(FileKey.OBJECT_DELIMITER) { it.format() }

/**
 * @return formatted string
 */
fun FileInfo.format(): String = "$name${FileKey.FIELD_DELIMITER}$uploadedMillis"

/**
 * @return [FileKey] created from [FileInfo]
 */
fun FileInfo.toFileKey(): FileKey = FileKey(this)
