/**
 * Configuration utilities for project which needs a runtime dependency to `save-agent`
 */

package com.saveourtool.save.buildutils

plugins {
    kotlin("jvm")
}

dependencies {
    addRuntimeDependency(
        "saveAgentPath",
        "save-agent",
        "copySaveAgent",
        "save-agent",
        this::add
    )
}
