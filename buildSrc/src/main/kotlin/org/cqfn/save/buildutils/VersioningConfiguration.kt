package org.cqfn.save.buildutils

import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

fun Project.configureVersioning() {
    apply<ReckonPlugin>()

    configure<ReckonExtension> {
        scopeFromProp()
        stageFromProp("rc", "final")  // use -Preckon.stage=final for release; otherwise version string will be based on last commit
    }
}
