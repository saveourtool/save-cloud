package com.saveourtool.save.demo.diktat

import com.saveourtool.save.demo.DemoAdditionalParams
import kotlinx.serialization.Serializable

/**
 * @property mode
 * @property tool
 * @property config
 */
@Serializable
data class DiktatDemoAdditionalParams(
    val mode: DiktatDemoMode,
    val tool: DiktatDemoTool,
    val config: String? = null,
) : DemoAdditionalParams
