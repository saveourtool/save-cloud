package com.saveourtool.save.demo.diktat

import com.saveourtool.save.demo.DemoAdditionalParams
import com.saveourtool.save.utils.Languages
import kotlinx.serialization.Serializable

/**
 * @property mode
 * @property tool
 * @property config
 */
@Serializable
data class DemoAdditionalParams(
    val mode: DiktatDemoMode = DiktatDemoMode.WARN,
    val tool: DiktatDemoTool = DiktatDemoTool.DIKTAT,
    val config: String? = null,
    val language: Languages = Languages.KOTLIN
) : DemoAdditionalParams
