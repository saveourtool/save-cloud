package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.entity.Demo

/**
 * Base interface for factory to create [CliRunner]
 */
interface CliRunnerFactory {
    /**
     * @param demo
     * @param version
     * @return [CliRunner] created for provided values
     */
    fun create(demo: Demo, version: String): CliRunner
}
