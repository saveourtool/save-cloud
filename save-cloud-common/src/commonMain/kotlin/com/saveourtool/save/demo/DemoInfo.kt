package com.saveourtool.save.demo

import kotlinx.serialization.Serializable

/**
 * @property demoToolRequest info required for demo setup and run
 * @property demoStatus current demo status
 */
@Serializable
data class DemoInfo(
    val demoToolRequest: NewDemoToolRequest,
    val demoStatus: DemoStatus,
)
