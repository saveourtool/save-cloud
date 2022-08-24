package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

/**
 * Enum of test type
 * @property pluginName
 */
@Serializable
@Suppress("CUSTOM_GETTERS_SETTERS")
enum class PluginType(val pluginName: String) {
    FIX("FixPlugin"),

    FIX_AND_WARN("FixAndWarnPlugin"),

    WARN("WarnPlugin"),
    ;

    companion object {
        val contestAllowedPlugins = listOf(WARN)
    }
}
