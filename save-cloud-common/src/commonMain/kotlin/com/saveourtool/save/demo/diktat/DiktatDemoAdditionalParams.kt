package com.saveourtool.save.demo.diktat

import com.saveourtool.save.demo.DemoAdditionalParams

/**
 * @property mode
 * @property tool
 * @property config
 */
data class DiktatDemoAdditionalParams(
    val mode: DiktatDemoMode,
    val tool: DiktatDemoTool,
    val config: String? = null,
) : DemoAdditionalParams
