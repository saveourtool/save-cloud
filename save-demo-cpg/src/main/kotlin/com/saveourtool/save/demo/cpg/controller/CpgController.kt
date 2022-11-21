package com.saveourtool.save.demo.cpg.controller

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.demo.cpg.StringResponse
import com.saveourtool.save.demo.cpg.config.ConfigProperties
import com.saveourtool.save.demo.diktat.DemoRunRequest
import com.saveourtool.save.utils.blockingToMono
import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.apache.commons.io.FileUtils
import org.neo4j.driver.exceptions.AuthenticationException
import org.neo4j.ogm.config.Configuration
import org.neo4j.ogm.exception.ConnectionException
import org.neo4j.ogm.session.Session
import org.neo4j.ogm.session.SessionFactory
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

const val FILE_NAME_SEPARATOR = "==="

private const val TIME_BETWEEN_CONNECTION_TRIES: Long = 6000
private const val MAX_RETRIES = 10
private const val DEFAULT_SAVE_DEPTH = -1

/**
 * A simple controller
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "cpg-demo"),
)
@RestController
@RequestMapping("/cpg/api")
class CpgController(
    val configProperties: ConfigProperties,
) {
    private val logger = LoggerFactory.getLogger(CpgController::class.java)

    /**
     * @param language
     * @param sourceCode
     * @return result of uploading, it contains ID to request the result further
     */
    @PostMapping("/upload-code")
    @OptIn(ExperimentalPython::class)
    fun uploadCode(
        @RequestBody request: DemoRunRequest,
    ): Mono<StringResponse> = blockingToMono {
        val tmpFolder = createTempDirectory(request.params.language.modeName)
        try {
            createFiles(request, tmpFolder)

            // creating the CPG configuration instance, it will be used to configure the graph
            val translationConfiguration =
                TranslationConfiguration.builder()
                    .topLevel(null)
                    // c++/java
                    .defaultLanguages()
                    // you can register non-default languages
                    .registerLanguage(PythonLanguageFrontend::class.java, listOf(".py"))
                    .debugParser(true)
                    // the directory with sources
                    .sourceLocations(tmpFolder.toFile())
                    .defaultPasses()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder()
                            .inferRecords(true)
                            .build()
                    )
                    .build()

            // result - is the parsed Code Property Graph
            val result = TranslationManager.builder()
                .config(translationConfiguration)
                .build()
                .analyze()
                .get()
            // commit the result to CPG
            saveTranslationResult(result)
        } finally {
            FileUtils.deleteDirectory(tmpFolder.toFile())
        }

        ResponseEntity.ok(tmpFolder.fileName.name)
    }

    /**
     * @param uploadId
     * @return result of translation
     */
    @GetMapping("/get-result")
    fun getResult(
        @RequestParam uploadId: String,
    ): ResponseEntity<String> = ResponseEntity.ok(
        """
            {
                "node": "value"
            }
        """.trimIndent()
    )

    private fun saveTranslationResult(result: TranslationResult) {
        val (session, factory) = connect()
        if (session == null) {
            logger.error("Cannot connect to a neo4j database")
            return
        }
        // FixMe: for each user we should keep the data
        session.purgeDatabase()

        session.beginTransaction().use {
            session.save(result.components, DEFAULT_SAVE_DEPTH)
            session.save(result.additionalNodes, DEFAULT_SAVE_DEPTH)
            it?.commit()
        }
        session.clear()
        factory?.close()
    }

    private fun connect(): Pair<Session?, SessionFactory?> {
        var fails = 0
        var sessionFactory: SessionFactory? = null
        var session: Session? = null
        while (session == null && fails < MAX_RETRIES) {
            try {
                val configuration =
                    Configuration.Builder()
                        .uri(configProperties.uri)
                        .autoIndex("none")
                        .credentials(configProperties.authentication.username, configProperties.authentication.password)
                        .verifyConnection(true)
                        .build()
                sessionFactory = SessionFactory(configuration, "de.frauhonfer.ais.cecpg.graph")
                session = sessionFactory.openSession()
            } catch (ex: ConnectionException) {
                sessionFactory = null
                fails++
                logger.error(
                    "Unable to connect to ${configProperties.uri}, " +
                            "ensure that the database is running and that " +
                            "there is a working network connection to it."
                )
                Thread.sleep(TIME_BETWEEN_CONNECTION_TRIES)
            } catch (ex: AuthenticationException) {
                logger.error("Unable to connect to ${configProperties.uri}, wrong username/password of the database")
            }
        }
        if (session == null) {
            logger.error("Unable to connect to ${configProperties.uri}")
        }
        return Pair(session, sessionFactory)
    }

    private fun createFiles(request: DemoRunRequest, tmpFolder: Path) {
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

    private data class SourceCodeFile(
        val name: String,
        val lines: MutableList<String>
    ) {
        private val logger = LoggerFactory.getLogger(SourceCodeFile::class.java)
        fun createSourceFile(tmpFolder: Path) {
            val file = (tmpFolder / name)
            file.writeLines(lines)
            logger.info("Created a file with sources: ${file.fileName}")
        }
    }
}
