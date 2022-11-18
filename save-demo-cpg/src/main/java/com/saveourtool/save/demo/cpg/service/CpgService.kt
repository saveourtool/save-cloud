package com.saveourtool.save.demo.cpg.service

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeLines

/**
 * Service for CPG
 */
@Service
class CpgService {
    private val tmpFilePrefix = mapOf(
        "java" to ("Test" to ".java"),
        "python" to ("test" to ".py"),
    )

    /**
     * @param language
     * @param sourceCode
     * @return result of translation Java code
     */
    fun translate(language: String, sourceCode: List<String>): TranslationResult {
        val tmpFolder = createTempDirectory(language)
        val tmpFile = tmpFilePrefix.getValue(language)
            .let { (prefix, suffix) ->
                createTempFile(tmpFolder, prefix, suffix)
                    .writeLines(sourceCode)
            }
        try {
            return TranslationManager.builder()
                .config(setupTranslationConfiguration(tmpFolder))
                .build()
                .analyze()
                .get()
        } finally {
            tmpFile.deleteIfExists()
        }
    }

    @OptIn(ExperimentalPython::class)
    private fun setupTranslationConfiguration(folder: Path): TranslationConfiguration = TranslationConfiguration.builder()
        .topLevel(null)
        .defaultLanguages()
        .registerLanguage(PythonLanguageFrontend::class.java, listOf(".py"))
        .debugParser(true)
        .sourceLocations(folder.toFile())
        .defaultPasses()
        .inferenceConfiguration(
            InferenceConfiguration.builder()
                .inferRecords(true)
                .build()
        )
        .build()
}
