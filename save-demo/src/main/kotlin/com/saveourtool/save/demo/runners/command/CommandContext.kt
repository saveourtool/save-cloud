package com.saveourtool.save.demo.runners.command

import com.saveourtool.save.demo.DemoMode
import java.nio.file.Path

/**
 * Context for command
 * @property testPath
 * @property tools
 * @property outputPath
 * @property mode
 * @property configPath
 */
data class CommandContext(
    val testPath: Path,
    val tools: Map<String, Path>,
    val outputPath: Path,
    val mode: DemoMode?,
    val configPath: Path?,
)
