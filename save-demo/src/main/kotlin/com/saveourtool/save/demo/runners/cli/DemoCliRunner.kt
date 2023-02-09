package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.utils.getLogger
import org.slf4j.Logger
import java.nio.file.Path

class DemoCliRunner(
    dependencyStorage: DependencyStorage,
    private val demo: Demo,
) : AbstractCliRunner(dependencyStorage), CliRunner {

    override fun getRunCommand(
        workingDir: Path,
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        demoRunRequest: DemoRunRequest
    ): String = demo.runCommand
        .replace(OUTPUT_PATH_PLACE_HOLDER, outputPath.toString())
        .let { runCommand ->
            configPath?.let {
                runCommand.replace(CONFIG_PLACE_HOLDER, it.toString())
            } ?: runCommand
        }
        .replace(TEST_PATH_PLACE_HOLDER, testPath.toString())

    override val log: Logger = Companion.log
    override val configName: String? = demo.configName

    companion object {
        private val log: Logger = getLogger<DiktatCliRunner>()
        const val OUTPUT_PATH_PLACE_HOLDER = "%%%OUTPUT_PATH%%%"
        const val TEST_PATH_PLACE_HOLDER = "%%%TEST_PATH%%%"
        const val CONFIG_PLACE_HOLDER = "%%%CONFIG%%%"
    }
}