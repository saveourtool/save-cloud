package com.saveourtool.save.entities.cosv

import kotlinx.serialization.Serializable

/**
 * DTO for statistics data for raw cosv file
 *
 * @property allAvailableFilesCount
 * @property uploadedArchivesCount
 * @property uploadedJsonFilesCount
 * @property processingFilesCount
 * @property duplicateFilesCount
 * @property errorFilesCount
 */
@Serializable
data class RawCosvFileStatisticsDto(
    val allAvailableFilesCount: Long,
    val uploadedArchivesCount: Long,
    val uploadedJsonFilesCount: Long,
    val processingFilesCount: Long,
    val duplicateFilesCount: Long,
    val errorFilesCount: Long,
)
