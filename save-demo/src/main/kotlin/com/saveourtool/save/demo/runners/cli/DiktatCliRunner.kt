package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.DemoMode
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.diktat.*
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.demo.storage.toToolKey
import com.saveourtool.save.demo.utils.*
import com.saveourtool.save.utils.getLogger

import io.ktor.util.*
import org.slf4j.Logger
import org.springframework.stereotype.Component

import java.nio.file.Path

import kotlin.io.path.*

/**
 * Class that allows to run diktat as command line application
 *
 * @property dependencyStorage
 */
@Component
class DiktatCliRunner(
    dependencyStorage: DependencyStorage,
    coroutineDispatchers: CustomCoroutineDispatchers,
) : AbstractCliRunner(dependencyStorage, coroutineDispatchers) {
    override val log: Logger = logger
    override val configName: String = DIKTAT_CONFIG_NAME
    override val testFileName: String = KOTLIN_TEST_NAME
    override val tmpDir: Path = createTempDirectory("diktat-demo-")

    override fun getRunCommand(
        workingDir: Path,
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        demoRunRequest: DemoRunRequest,
    ): String = buildString {
        // TODO: this information should not be hardcoded but stored in database
        val ktlintExecutable = getExecutable(workingDir, DiktatDemoTool.KTLINT.toToolKey("ktlint"))
        append(ktlintExecutable)

        val diktatExecutable = getExecutable(workingDir, DiktatDemoTool.DIKTAT.toToolKey("diktat-1.2.3.jar"))
        append(" -R $diktatExecutable ")
        // disabling package-naming as far as it is useless in demo because there is no folder hierarchy
        append(" --disabled_rules=diktat-ruleset:package-naming,standard")

        append(" --reporter=plain,output=$outputPath ")
        if (demoRunRequest.mode == DemoMode.FIX) {
            append(" --format ")
        }
        append(testPath)
    }

    companion object {
        private val logger: Logger = getLogger<DiktatCliRunner>()
    }
}
