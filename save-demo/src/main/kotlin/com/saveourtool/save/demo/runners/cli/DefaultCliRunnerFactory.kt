package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.runners.command.CommandBuilder
import com.saveourtool.save.demo.storage.DependencyStorage
import org.springframework.stereotype.Component

/**
 * Factory creates [DemoCliRunner]
 */
@Component
class DefaultCliRunnerFactory(
    private val dependencyStorage: DependencyStorage,
    private val commandBuilder: CommandBuilder,
) : CliRunnerFactory {
    override fun create(demo: Demo, version: String): CliRunner = DemoCliRunner(dependencyStorage, commandBuilder, demo, version)
}
