package org.cqfn.save.api

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import org.cqfn.save.execution.ExecutionType

import java.lang.IllegalArgumentException

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

fun main(args: Array<String>) {
    val mode = parseArguments(args) ?: return

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
        automaticTestInitializator.start(mode)
    }
}

private fun parseArguments(args: Array<String>): ExecutionType? {
    if (args.isEmpty()) {
        log.error("Argument list couldn't be empty!")
        return null
    }
    val parser = ArgParser("")
    val mode by parser.option(
        ArgType.Choice<ExecutionType>(),
        fullName = "mode",
        shortName = "m",
        description = "Mode of execution GIT/STANDARD"
    )
    parser.parse(args)
    return mode
}
