@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.apicli

import com.saveourtool.save.api.SaveCloudClient
import com.saveourtool.save.api.config.EvaluatedToolProperties
import com.saveourtool.save.api.config.PropertiesConfigurationType
import com.saveourtool.save.api.config.WebClientProperties

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

    val saveCloudClient = SaveCloudClient(
        webClientProperties,
        evaluatedToolProperties,
        cliArgs.mode,
        cliArgs.authorization
    )

    runBlocking {
        saveCloudClient.start()
    }
}
