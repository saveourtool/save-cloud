package com.saveourtool.save.buildutils

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.*

import java.time.Duration

plugins {
    id("de.undercouch.download")
}

configureVersioning()

val saveCoreVersion = the<LibrariesForLibs>()
    .versions
    .save
    .core
    .get()
tasks.register("getSaveCliVersion") {
    // description = "Reads version of save-cli, either from project property, or from Versions, or latest"
    inputs.property("save-cli version", findProperty("saveCliVersion") ?: saveCoreVersion)
    val file = file(pathToSaveCliVersion)
    outputs.file(file)
    outputs.upToDateWhen {
        // cache value of latest save-cli version for 10 minutes to keep request rate to Github reasonable
        (System.currentTimeMillis() - file.lastModified()) < Duration.ofMinutes(10).toMillis()
    }
    doFirst {
        file.writeText("""version=$saveCoreVersion""")
    }
}
