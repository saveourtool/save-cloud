package com.saveourtool.save.demo.cpg.service

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import org.springframework.stereotype.Service
import java.nio.file.Path

import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeLines

@Service
class CpgService {
    fun translate(sourceCode: List<String>): TranslationResult {
        val tmpFile = createTempFile("Test", ".java")
            .writeLines(sourceCode)
        try {
            return TranslationManager.builder()
                .config(setupTranslationConfiguration(tmpFile))
                .build()
                .analyze()
                .get()
        } finally {
            tmpFile.deleteIfExists()
        }
    }

    private fun setupTranslationConfiguration(filePath: Path): TranslationConfiguration {
        return TranslationConfiguration.builder()
            .topLevel(null)
            .defaultLanguages()
            .debugParser(true)
            .sourceLocations(filePath.toFile())
            .defaultPasses()
            .inferenceConfiguration(
                InferenceConfiguration.builder()
                    .inferRecords(true)
                    .build()
            )
            .build()
    }
}