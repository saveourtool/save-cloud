package com.saveourtool.save.demo.cpg

import com.saveourtool.save.utils.Languages
import kotlinx.serialization.Serializable

/**
 * Data class that represents all additional params required to run CpgDemo
 *
 * @property language language that the code for demo was written in
 */
@Serializable
data class CpgAdditionalParams(
    val language: Languages = Languages.KOTLIN,
)
