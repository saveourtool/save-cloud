package com.saveourtool.save.demo.cpg.controller

import com.saveourtool.save.configs.ApiSwaggerSupport
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
import java.io.File
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*
import kotlin.system.exitProcess

const val FILE_NAME_SEPARATOR = "==="


private const val S_TO_MS_FACTOR = 1000
private const val TIME_BETWEEN_CONNECTION_TRIES: Long = 2000
private const val MAX_COUNT_OF_FAILS = 10
private const val EXIT_SUCCESS = 0
private const val EXIT_FAILURE = 1
private const val VERIFY_CONNECTION = true
private const val DEBUG_PARSER = true
private const val AUTO_INDEX = "none"
private const val PROTOCOL = "bolt://"

private const val DEFAULT_HOST = "localhost"
private const val DEFAULT_PORT = 7687
private const val DEFAULT_USER_NAME = "neo4j"
private const val DEFAULT_PASSWORD = "123"
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
class CpgController {
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
    ) = blockingToMono {
        val tmpFolder = createTempDirectory(request.params.language.modeName)
        try {
            createFiles(request, tmpFolder)

            val translationConfiguration =
                TranslationConfiguration.builder()
                    .topLevel(null)
                    .defaultLanguages()
                    .registerLanguage(PythonLanguageFrontend::class.java, listOf(".py"))
                    .debugParser(true)
                    .sourceLocations(tmpFolder.toFile())
                    .defaultPasses()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder()
                            .inferRecords(true)
                            .build()
                    )
                    .build()

            val result = TranslationManager.builder()
                .config(translationConfiguration)
                .build()
                .analyze()
                .get()

            saveTranslationResult(result)
        } finally {
            FileUtils.deleteDirectory(tmpFolder.toFile())
        }
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
        session?.purgeDatabase()

        session?.beginTransaction().use {
            session?.save(result.components, DEFAULT_SAVE_DEPTH)
            session?.save(result.additionalNodes, DEFAULT_SAVE_DEPTH)
            it?.commit()
        }
        session?.clear()
        factory?.close()
    }
    private fun connect(): Pair<Session?, SessionFactory?> {
        var fails = 0
        var sessionFactory: SessionFactory? = null
        var session: Session? = null
        while (session == null && fails < MAX_COUNT_OF_FAILS) {
            try {
                // FixMe: change this code, no default passwords should be here
                val configuration =
                    Configuration.Builder()
                        .uri("$PROTOCOL$DEFAULT_HOST:$DEFAULT_PORT")
                        .autoIndex(AUTO_INDEX)
                        .credentials(DEFAULT_USER_NAME, DEFAULT_PASSWORD)
                        .verifyConnection(VERIFY_CONNECTION)
                        .build()
                sessionFactory = SessionFactory(configuration, "de.fraunhofer.aisec.cpg.graph")
                session = sessionFactory.openSession()
            } catch (ex: ConnectionException) {
                sessionFactory = null
                fails++
                logger.error(
                    "Unable to connect to localhost:7687, " +
                            "ensure the database is running and that " +
                            "there is a working network connection to it."
                )
                Thread.sleep(TIME_BETWEEN_CONNECTION_TRIES)
            } catch (ex: AuthenticationException) {
                logger.error("Unable to connect to localhost:7687, wrong username/password!")
            }
        }
        if (session == null) {
            logger.error("Unable to connect to localhost:7687")
        }
        return Pair(session, sessionFactory)
    }

    private fun createFiles(request: DemoRunRequest, tmpFolder: Path) {
        val files: MutableList<SourceCodeFile> = mutableListOf()
        request.codeLines.forEach {
            if (it.startsWith(FILE_NAME_SEPARATOR) && it.endsWith(FILE_NAME_SEPARATOR)) {
                files.add(SourceCodeFile(it.getFileName(), mutableListOf()))
            } else {
                files.last().lines.add(it)
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
