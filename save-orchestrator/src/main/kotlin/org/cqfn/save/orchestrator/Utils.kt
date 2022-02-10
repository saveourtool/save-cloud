/**
 * Utilities for orchestrator
 */

package org.cqfn.save.orchestrator

/**
 * Create synthetic toml config for standard mode in aim to execute all suites at the same time
 *
 * @param execCmd execCmd for SAVE-cli for testing in standard mode
 * @param batchSizeForAnalyzer batchSize for SAVE-cli for testing in standard mode
 * @return synthetic toml config data
 */
// FixMe: Use serialization after ktoml upgrades
fun createSyntheticTomlConfig(execCmd: String?, batchSizeForAnalyzer: String?): String {
    val exeCmdForTomlConfig = if (execCmd.isNullOrBlank()) "" else "execCmd = \"$execCmd\""
    val batchSizeForTomlConfig = if (batchSizeForAnalyzer.isNullOrBlank()) {
        ""
    } else {
        """
        |[fix]
        |    batchSize = $batchSizeForAnalyzer
        |[warn]
        |    batchSize = $batchSizeForAnalyzer
        """.trimMargin()
    }
    return """
           |[general]
           |$exeCmdForTomlConfig
           |$batchSizeForTomlConfig
           """.trimMargin()
}
