package com.saveourtool.save.demo.cpg.controller

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.demo.cpg.*
import com.saveourtool.save.demo.cpg.config.ConfigProperties
import com.saveourtool.save.demo.cpg.utils.*
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.info

import arrow.core.getOrHandle
import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.apache.commons.io.FileUtils
import org.neo4j.ogm.response.model.RelationshipModel
import org.neo4j.ogm.session.Session
import org.slf4j.Logger
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.util.*

import kotlin.io.path.*
import kotlinx.serialization.ExperimentalSerializationApi

const val FILE_NAME_SEPARATOR = "==="

private const val TIME_BETWEEN_CONNECTION_TRIES: Long = 6000
private const val MAX_RETRIES = 10
private const val DEFAULT_SAVE_DEPTH = -1

/**
 * A simple controller
 * @property configProperties
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "cpg-demo"),
)
@RestController
@RequestMapping("/cpg/api")
@ExperimentalSerializationApi
class CpgController(
    val configProperties: ConfigProperties,
) {
    /**
     * @param request
     * @return result of uploading, it contains ID to request the result further
     */
    @PostMapping("/upload-code")
    fun uploadCode(
        @RequestBody request: CpgRunRequest,
    ): Mono<CpgResult> = blockingToMono {
        val tmpFolder = createTempDirectory(request.params.language.modeName)
        var logs = mutableListOf<String>()
        try {
            // creating temporary folder for the input
            createFiles(request, tmpFolder)
            val (result, logsFromLogback) = LogbackCapturer(BASE_PACKAGE_NAME) {
                // creating the CPG configuration instance, it will be used to configure the graph
                val translationConfiguration = createTranslationConfiguration(tmpFolder)

                // result - is the parsed Code Property Graph
                TranslationManager.builder()
                    .config(translationConfiguration)
                    .build()
                    .analyze()
                    .get()
            }

            logs = logsFromLogback.toMutableList()
            result
                .tap {
                    saveTranslationResult(it)
                }
                .map {
                    CpgResult(
                        getGraph(),
                        tmpFolder.fileName.name,
                        logs,
                    )
                }
                .getOrHandle {
                    logs += "Exception: ${it.message} ${it.stackTraceToString()}"
                    CpgResult(
                        CpgGraph.placeholder,
                        "Error happened during the parsing of code to CPG",
                        logs,
                    )
                }
        } catch (e: Exception) {
            logs += "Exception: ${e.message} ${e.stackTraceToString()}"
            logs.stubCpgResult("Error happened on read/write from/to a graph database")
        } finally {
            FileUtils.deleteDirectory(tmpFolder.toFile())
        }
    }

    private fun List<String>.stubCpgResult(error: String) =
        CpgResult(
            CpgGraph.placeholder,
            error,
            this,
        )

    @OptIn(ExperimentalPython::class)
    private fun createTranslationConfiguration(folder: Path): TranslationConfiguration =
        TranslationConfiguration.builder()
            .topLevel(null)
            // c++/java
            .defaultLanguages()
            // you can register non-default languages
            .registerLanguage(PythonLanguageFrontend::class.java, listOf(".py"))
            .debugParser(true)
            // the directory with sources
            .sourceLocations(folder.toFile())
            .defaultPasses()
            .inferenceConfiguration(
                InferenceConfiguration.builder()
                    .inferRecords(true)
                    .build()
            )
            .build()

    private fun getGraph(): CpgGraph {
        val (nodes, edges) = connect().use { session ->
            session.getNodes() to session.getEdges()
        }
        return CpgGraph(nodes = nodes.map { it.toCpgNode() }, edges = edges.map { it.toCpgEdge() })
    }

    private fun Session.getNodes() = query(Node::class.java, "MATCH (n: Node) return n", mapOf("" to "")).toList()

    private fun Session.getEdges() = query("MATCH () -[r]-> () return r", mapOf("" to ""))
        .map {
            it.values
        }
        .flatten()
        .map {
            it as RelationshipModel
        }

    private fun saveTranslationResult(result: TranslationResult) {
        val sessionWithFactory = connect()

        log.info { "Using import depth: $DEFAULT_SAVE_DEPTH" }
        log.info {
            "Count base nodes to save [components: ${result.components.size}, additionalNode: ${result.additionalNodes.size}]"
        }

        sessionWithFactory.use { session ->
            session.beginTransaction().use {
                session.save(result.components, DEFAULT_SAVE_DEPTH)
                session.save(result.additionalNodes, DEFAULT_SAVE_DEPTH)
                it?.commit()
            }
        }
    }

    private fun connect(): SessionWithFactory = retry(MAX_RETRIES, TIME_BETWEEN_CONNECTION_TRIES) {
        tryConnect(
            configProperties.uri,
            configProperties.authentication.username,
            configProperties.authentication.password,
            MODEL_PACKAGE_NAME,
        )
    }

    private fun createFiles(request: CpgRunRequest, tmpFolder: Path) {
        val files: MutableList<SourceCodeFile> = mutableListOf()
        request.codeLines.filterNot { it.isBlank() }.forEachIndexed { index, line ->
            if (line.startsWith(FILE_NAME_SEPARATOR) && line.endsWith(FILE_NAME_SEPARATOR)) {
                files.add(SourceCodeFile(line.getFileName(), mutableListOf()))
            } else {
                if (index == 0) {
                    files.add(SourceCodeFile("demo${request.params.language.extension}", mutableListOf()))
                }
                files.last().lines.add(line)
            }
        }
        files.forEach {
            it.createSourceFile(tmpFolder)
        }
    }

    private fun String.getFileName() =
        this.trim()
            .drop(FILE_NAME_SEPARATOR.length)
            .dropLast(FILE_NAME_SEPARATOR.length)
            .trim()

    /**
     * @property name
     * @property lines
     */
    private data class SourceCodeFile(
        val name: String,
        val lines: MutableList<String>
    ) {
        /**
         * @param tmpFolder
         */
        fun createSourceFile(tmpFolder: Path) {
            val file = (tmpFolder / name)
            file.writeLines(lines)
            log.info("Created a file with sources: ${file.fileName}")
        }
    }

    companion object {
        private val log: Logger = getLogger<CpgController>()
        private const val BASE_PACKAGE_NAME = "de.fraunhofer.aisec"
        private const val MODEL_PACKAGE_NAME = "$BASE_PACKAGE_NAME.cpg.graph"
    }
}
