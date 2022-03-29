@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.api

import java.lang.IllegalArgumentException

import kotlinx.coroutines.runBlocking

@Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
fun main(args: Array<String>) {
    val cliArgs = parseArguments(args) ?: return

    val webClientPropertiesFileName = "web-client.properties"
    val evaluatedToolPropertiesFileName = "evaluated-tool.properties"

    val webClientProperties = readPropertiesFile(webClientPropertiesFileName, PropertiesConfigurationType.WEB_CLIENT) as WebClientProperties?
    val evaluatedToolProperties = readPropertiesFile(evaluatedToolPropertiesFileName, PropertiesConfigurationType.EVALUATED_TOOL) as EvaluatedToolProperties?

    if (webClientProperties == null || evaluatedToolProperties == null) {
        throw IllegalArgumentException(
            "Configuration for web client and for evaluate tool couldn't be empty!" +
                    " Please make sure, that you have proper configuration in files: $webClientPropertiesFileName, $evaluatedToolPropertiesFileName"
        )
    }

    val automaticTestInitializator = AutomaticTestInitializator(webClientProperties, evaluatedToolProperties)

    runBlocking {
        automaticTestInitializator.start(cliArgs)
    }
}
