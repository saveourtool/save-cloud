package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.DtoWithId
import com.saveourtool.save.utils.ARCHIVE_EXTENSION
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * DTO for raw cosv file
 *
 * @property fileName
 * @property userName
 * @property organizationName
 * @property status
 * @property statusMessage
 * @property updateDate
 * @property contentLength
 * @property id
 */
@Serializable
data class RawCosvFileDto(
    val fileName: String,
    val userName: String,
    val organizationName: String,
    val status: RawCosvFileStatus = RawCosvFileStatus.UPLOADED,
    val statusMessage: String? = null,
    val updateDate: LocalDateTime? = null,
    val contentLength: Long? = null,
    override val id: Long? = null,
) : DtoWithId() {
    /**
     * @return non-nullable [contentLength]
     */
    fun requiredContentLength(): Long = requireNotNull(contentLength) {
        "contentLength is not provided: $this"
    }

    companion object {
        /**
         * @return [Boolean]
         */
        fun RawCosvFileDto.isArchive() = status == RawCosvFileStatus.UPLOADED && fileName.endsWith(ARCHIVE_EXTENSION, ignoreCase = true)

        /**
         * @return [Boolean]
         */
        fun RawCosvFileDto.isJsonFile() = status == RawCosvFileStatus.UPLOADED && !fileName.endsWith(ARCHIVE_EXTENSION, ignoreCase = true)

        /**
         * @return [Boolean]
         */
        fun RawCosvFileDto.isProcessing() = status == RawCosvFileStatus.PROCESSED || status == RawCosvFileStatus.IN_PROGRESS

        /**
         * @return [Boolean]
         */
        fun RawCosvFileDto.isDuplicate() = status == RawCosvFileStatus.FAILED && statusMessage?.contains("Duplicate") == true

        /**
         * @return [Boolean]
         */
        fun RawCosvFileDto.isErrorFile() = status == RawCosvFileStatus.FAILED && statusMessage?.contains("Duplicate") == false
    }
}
