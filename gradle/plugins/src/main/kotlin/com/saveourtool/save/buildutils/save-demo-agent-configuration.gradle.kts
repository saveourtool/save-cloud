/**
 * Configuration utilities for project which needs a runtime dependency to `save-demo-agent`
 */

package com.saveourtool.save.buildutils

plugins {
    kotlin("jvm")
}

dependencies {
    addRuntimeDependency(
        "saveDemoAgentPath",
        "save-demo-agent",
        "copySaveDemoAgent",
        "save-demo-agent",
        this::add
    )
}
