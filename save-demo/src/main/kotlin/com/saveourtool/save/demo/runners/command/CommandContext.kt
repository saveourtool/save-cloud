package com.saveourtool.save.demo.runners.command

import java.nio.file.Path

/**
 * Context for command
 *
 * @property testPath
 * @property tools
 * @property outputPath
 * @property configPath
 */
data class CommandContext(
    val testPath: Path,
    val tools: Map<String, Path>,
    val outputPath: Path,
    val configPath: Path?,
)
