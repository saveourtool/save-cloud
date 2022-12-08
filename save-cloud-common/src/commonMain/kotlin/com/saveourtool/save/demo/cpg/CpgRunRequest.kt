package com.saveourtool.save.demo.cpg

import com.saveourtool.save.demo.DemoRequest
import kotlinx.serialization.Serializable

/**
 * Data class that represents the request to run CPG demo
 *
 * @property codeLines file as String that contains code requested for cpg demo run
 * @property params all the extra params required for cpg demo run
 */
@Serializable
data class CpgRunRequest(
    val codeLines: List<String>,
    val params: CpgAdditionalParams,
) : DemoRequest
