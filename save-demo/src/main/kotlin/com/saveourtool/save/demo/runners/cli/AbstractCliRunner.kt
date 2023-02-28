package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.utils.LOG_FILE_NAME
import com.saveourtool.save.demo.utils.REPORT_FILE_NAME
import com.saveourtool.save.demo.utils.prependPath
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.collectToFile
import com.saveourtool.save.utils.orNotFound
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import reactor.core.publisher.Mono
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path
import kotlin.io.path.*

/**
 * @property dependencyStorage
 */
abstract class AbstractCliRunner(
    protected val dependencyStorage: DependencyStorage,
    private val coroutineDispatchers: CustomCoroutineDispatchers,
) : CliRunner {
    /**
     * logger from child
     */
    protected abstract val log: Logger

    /**
     * configName, can be missed
     */
    protected abstract val configName: String?

    override fun run(testPath: Path, demoRunRequest: DemoRunRequest): Mono<DemoResult> = blockingToMono {
        val workingDir = testPath.parent
        val outputPath = workingDir / REPORT_FILE_NAME
        val configPath = demoRunRequest.config?.joinToString("\n")
            ?.let { configContent ->
                val requiredConfigName = requireNotNull(configName) {
                    "config is provided for cli runner which doesn't support it"
                }
                prepareFile(workingDir / requiredConfigName, configContent)
            }
        val launchLogPath = workingDir / LOG_FILE_NAME
        val command = getRunCommand(workingDir, testPath, outputPath, configPath, demoRunRequest)
        val processBuilder = createProcessBuilder(command).apply {
            redirectErrorStream(true)
            redirectOutput(ProcessBuilder.Redirect.appendTo(launchLogPath.toFile()))

            /*
             * Inherit JAVA_HOME for the child process.
             */
            val javaHome = System.getProperty("java.home")
            environment()["JAVA_HOME"] = javaHome
            /*
             * Need to remove JAVA_TOOL_OPTIONS (and _JAVA_OPTIONS just in case) because JAVA_TOOL_OPTIONS is set by spring,
             * so it breaks ktlint's "java -version" parsing (for ktlint 0.46.1, fixed in ktlint 0.47.1)
             */
            environment().remove("JAVA_TOOL_OPTIONS")
            environment().remove("_JAVA_OPTIONS")
            prependPath(Path(javaHome) / "bin")
        }

        log.debug("Running command [$command].")
        val terminationCode = processBuilder.start().waitFor()

        val logs = launchLogPath.readLines()

        val warnings = try {
            outputPath.readLines()
        } catch (e: IOException) {
            logs.forEach(log::error)
            throw UncheckedIOException(e)
        }

        logs.forEach(log::trace)

        log.trace("Found ${warnings.size} warning(s): [${warnings.joinToString(", ")}]")

        DemoResult(
            warnings.map { it.replace(testPath.absolutePathString(), testPath.name) },
            testPath.readLines(),
            logs,
            terminationCode,
        )
    }

    override fun getExecutable(workingDir: Path, toolKey: ToolKey): Path = runBlocking(coroutineDispatchers.default) {
        with(toolKey) {
            dependencyStorage.findDependency(organizationName, projectName, version, fileName)
        }
            .orNotFound {
                "Could not find file with key $toolKey"
            }
            .let { dependencyStorage.download(it) }
            .collectToFile(workingDir / toolKey.fileName)
            .let { workingDir / toolKey.fileName }
            .also { it.toFile().setExecutable(true, false) }
    }
}
