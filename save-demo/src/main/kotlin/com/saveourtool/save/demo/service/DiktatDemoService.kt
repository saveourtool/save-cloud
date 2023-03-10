package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.runners.cli.DiktatCliRunner

import org.springframework.stereotype.Service

/**
 * Demo service implementation for diktat-demo
 */
@Service
class DiktatDemoService(diktatCliRunner: DiktatCliRunner) : AbstractDemoService(diktatCliRunner)
