package com.saveourtool.save.buildutils

import org.codehaus.groovy.runtime.ResourceGroovyMethods
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.*

import java.net.URL
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
        val version = if (saveCoreVersion.isSnapshot()) {
            // try to get the required version of cli
            findProperty("saveCliVersion") as String? ?: run {
                // as fallback, use latest release to allow the project to build successfully
                val latestRelease = ResourceGroovyMethods.getText(
                    URL("https://api.github.com/repos/saveourtool/save-cli/releases/latest")
                )
                (groovy.json.JsonSlurper().parseText(latestRelease) as Map<String, Any>)["tag_name"].let {
                    (it as String).trim('v')
                }
            }
        } else {
            saveCoreVersion
        }
        file.writeText("""version=$version""")
    }
}
