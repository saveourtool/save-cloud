package com.saveourtool.save.demo.runners.command

import com.saveourtool.save.demo.DemoMode
import org.apache.commons.text.StringSubstitutor
import org.springframework.stereotype.Component

/**
 * Component to build a command line
 */
@Component
class CommandBuilder {
    /**
     * @param templateCommand
     * @param commandContext
     * @return command line built by provided values
     */
    fun build(templateCommand: String, commandContext: CommandContext): String =
            StringSubstitutor.replace(templateCommand, commandContext.toMap())

    private fun CommandContext.toMap(): Map<String, String> = buildMap {
        put("testPath", testPath.toString())
        put("outputPath", outputPath.toString())
        mode?.let {
            when (it) {
                DemoMode.FIX -> put("isWarn", "")
                DemoMode.WARN -> put("isFix", "")
            }
        }
        tools.map { (key, value) ->
            put("tools.$key", value.toString())
        }
        configPath?.let {
            put("configPath", it.toString())
        }
    }
}
