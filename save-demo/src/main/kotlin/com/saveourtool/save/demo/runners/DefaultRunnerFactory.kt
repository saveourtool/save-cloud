package com.saveourtool.save.demo.runners

import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.runners.RunnerFactory.RunnerType
import com.saveourtool.save.demo.runners.cli.DemoCliRunner
import com.saveourtool.save.demo.runners.command.CommandBuilder
import com.saveourtool.save.demo.runners.pod.DemoPodRunner
import com.saveourtool.save.demo.service.KubernetesService
import com.saveourtool.save.demo.storage.DependencyStorage

import org.springframework.stereotype.Component

/**
 * Factory creates both [DemoCliRunner] and [DemoPodRunner].
 */
@Component
class DefaultRunnerFactory(
    private val dependencyStorage: DependencyStorage,
    private val commandBuilder: CommandBuilder,
    private val kubernetesService: KubernetesService,
) : RunnerFactory {
    override fun create(demo: Demo, version: String, type: RunnerType): Runner = when (type) {
        RunnerType.CLI -> DemoCliRunner(dependencyStorage, commandBuilder, demo, version)
        RunnerType.POD -> DemoPodRunner(kubernetesService, demo)
        RunnerType.PURE -> throw NotImplementedError("Pure runner should not be used. Try Cli or Pod instead.")
    }
}
