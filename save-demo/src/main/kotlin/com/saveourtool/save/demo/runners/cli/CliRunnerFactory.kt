package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.entity.Demo

interface CliRunnerFactory {
    fun create(demo: Demo): CliRunner
}