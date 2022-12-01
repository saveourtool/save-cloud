package com.saveourtool.save.demo.cpg

import com.saveourtool.save.demo.DemoResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @property cpgGraph
 * @property logs
 * @property applicationName
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class CpgResult(
    val cpgGraph: CpgGraph,
    val applicationName: String,
    val logs: List<String>,
) : DemoResult
