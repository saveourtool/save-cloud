package com.saveourtool.save.orchestrator

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class UtilsTest {
    private val defaultConfig = """
                                |[general]
                                |
                                |
                                """.trimMargin()
    private val configWithExecCmd = """
                                    |[general]
                                    |execCmd = "java -jar ktlint -R diktat.jar"
                                    |
                                    """.trimMargin()
    private val configWithBatchSize = """
                                      |[general]
                                      |
                                      |[fix]
                                      |    batchSize = 42
                                      |[warn]
                                      |    batchSize = 42
                                      """.trimMargin()
    private val configWithExecCmdAndBatchSize = """
                                                |[general]
                                                |execCmd = "java -jar ktlint -R diktat.jar"
                                                |[fix]
                                                |    batchSize = 42
                                                |[warn]
                                                |    batchSize = 42
                                                """.trimMargin()

    @Test
    fun `test default config 1`() {
        val execCmd = null
        val batchSize = null

        val configData = createSyntheticTomlConfig(execCmd, batchSize)

        assertEquals(configData, defaultConfig)
    }

    @Test
    fun `test default config 2`() {
        val execCmd = ""
        val batchSize = null

        val configData = createSyntheticTomlConfig(execCmd, batchSize)

        assertEquals(configData, defaultConfig)
    }

    @Test
    fun `test config with execCmd`() {
        val execCmd = "java -jar ktlint -R diktat.jar"
        val batchSize = ""

        val configData = createSyntheticTomlConfig(execCmd, batchSize)

        assertEquals(configData, configWithExecCmd)
    }

    @Test
    fun `test config with batch size`() {
        val execCmd = ""
        val batchSize = "42"

        val configData = createSyntheticTomlConfig(execCmd, batchSize)

        assertEquals(configData, configWithBatchSize)
    }

    @Test
    fun `test config execCmd and batch size`() {
        val execCmd = "java -jar ktlint -R diktat.jar"
        val batchSize = "42"

        val configData = createSyntheticTomlConfig(execCmd, batchSize)

        assertEquals(configData, configWithExecCmdAndBatchSize)
    }
}
