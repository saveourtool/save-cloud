package com.saveourtool.save.demo

import kotlinx.serialization.Serializable

/**
 * @property demoDto info required for demo setup and run
 * @property demoStatus current demo status
 */
@Serializable
data class DemoInfo(
    val demoDto: DemoDto,
    val demoStatus: DemoStatus,
)
