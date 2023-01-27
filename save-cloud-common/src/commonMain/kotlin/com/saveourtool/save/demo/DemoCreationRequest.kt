package com.saveourtool.save.demo

import com.saveourtool.save.entities.FileDto
import kotlinx.serialization.Serializable

/**
 * @property demoDto
 * @property manuallyUploadedFileDtos
 */
@Serializable
data class DemoCreationRequest(
    val demoDto: DemoDto,
    val manuallyUploadedFileDtos: List<FileDto>,
)
