package org.cqfn.save.api

import kotlinx.coroutines.runBlocking

fun main() {
    // TODO: Should be used as CLI argument for application
    val webClientPropertiesFileName = "web-client.properties"
    val evaluatedToolPropertiesFileName = "evaluated-tool.properties"

    val automaticTestInitializator = AutomaticTestInitializator(webClientPropertiesFileName, evaluatedToolPropertiesFileName)

    runBlocking {
        automaticTestInitializator.start()
    }
}
