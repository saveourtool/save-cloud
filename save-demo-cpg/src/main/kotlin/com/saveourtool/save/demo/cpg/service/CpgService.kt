package com.saveourtool.save.demo.cpg.service

import com.saveourtool.save.demo.cpg.utils.LogbackCapturer
import com.saveourtool.save.demo.cpg.utils.ResultWithLogs
import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.name

/**
 * Service which communicates with CPG
 */
@Service
class CpgService {
    /**
     * Translate all code in provided folder
     *
     * @param folder
     * @return result from CPG with logs
     */
    fun translate(folder: Path): ResultWithLogs<TranslationResult> = LogbackCapturer(BASE_PACKAGE_NAME) {
        // creating the CPG configuration instance, it will be used to configure the graph
        val translationConfiguration = createTranslationConfiguration(folder, folder.fileName.name)

        // result - is the parsed Code Property Graph
        TranslationManager.builder()
            .config(translationConfiguration)
            .build()
            .analyze()
            .get()
    }

    @OptIn(ExperimentalPython::class)
    private fun createTranslationConfiguration(folder: Path, applicationName: String): TranslationConfiguration = TranslationConfiguration.builder()
        .topLevel(null)
        // c++/java
        .defaultLanguages()
        // you can register non-default languages
        .registerLanguage(PythonLanguageFrontend::class.java, listOf(".py"))
        .debugParser(true)
        // the directory with sources
        .softwareComponents(mutableMapOf(applicationName to listOf(folder.toFile())))
        .defaultPasses()
        .inferenceConfiguration(
            InferenceConfiguration.builder()
                .inferRecords(true)
                .build()
        )
        .build()

    companion object {
        private const val BASE_PACKAGE_NAME = "de.fraunhofer.aisec"
    }
}
