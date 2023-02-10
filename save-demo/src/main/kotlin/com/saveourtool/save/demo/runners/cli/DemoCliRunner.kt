package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.runners.command.CommandBuilder
import com.saveourtool.save.demo.runners.command.CommandContext
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.demo.storage.toToolKey
import com.saveourtool.save.utils.getLogger
import org.slf4j.Logger
import java.nio.file.Path

/**
 * [CliRunner] for provided [demo] with specific [version]
 */
class DemoCliRunner(
    dependencyStorage: DependencyStorage,
    private val commandBuilder: CommandBuilder,
    private val demo: Demo,
    private val version: String,
) : AbstractCliRunner(dependencyStorage), CliRunner {
    override val log: Logger = Companion.log
    override val configName: String? = demo.configName
    override fun getRunCommand(
        workingDir: Path,
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        demoRunRequest: DemoRunRequest
    ): String {
        // download probably should be moved to another place
        val tools = dependencyStorage.blockingList(demo, version)
            .associate { dependency ->
                dependency.fileName to getExecutable(workingDir, dependency.toToolKey())
            }
        return commandBuilder.build(
            demo.runCommand,
            CommandContext(
                testPath,
                tools,
                outputPath,
                demoRunRequest.mode,
                configPath,
            )
        )
    }

    companion object {
        private val log: Logger = getLogger<DiktatCliRunner>()
    }
}
