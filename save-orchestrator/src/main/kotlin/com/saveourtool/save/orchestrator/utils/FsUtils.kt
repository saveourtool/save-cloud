package com.saveourtool.save.orchestrator.utils

import com.saveourtool.save.orchestrator.controller.AgentsController
import com.saveourtool.save.utils.warn
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

internal fun Path.tryMarkAsExecutable() {
    try {
        Files.setPosixFilePermissions(this, Files.getPosixFilePermissions(this) + AgentsController.allExecute)
    } catch (ex: UnsupportedOperationException) {
        AgentsController.log.warn(ex) { "Failed to mark file ${this.name} as executable" }
    }
}
