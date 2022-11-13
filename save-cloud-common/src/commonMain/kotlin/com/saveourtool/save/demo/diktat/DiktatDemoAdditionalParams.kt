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
    val mode: DiktatDemoMode = DiktatDemoMode.WARN,
    val tool: DiktatDemoTool = DiktatDemoTool.DIKTAT,
    val config: String? = null,
) : DemoAdditionalParams
