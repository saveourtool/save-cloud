package com.saveourtool.save.preprocessor.config

/**
 * Class for repositories with standard test suites
 *
 * @property gitUrl git url of repo
 * @property gitBranchOrCommit git branch or commit in repo
 * @property testSuitePaths list of test suite root paths
 */
data class TestSuitesRepo(
    val gitUrl: String,
    val gitBranchOrCommit: String,
    val testSuitePaths: List<String>,
)
