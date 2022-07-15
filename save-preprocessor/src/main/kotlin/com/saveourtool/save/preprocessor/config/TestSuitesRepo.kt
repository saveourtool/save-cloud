package com.saveourtool.save.preprocessor.config

/**
 * Class for repositories with standard test suites
 *
 * @property organizationName name of organization for standard test suites
 * @property url git url of repo
 * @property branch git branch or commit in repo
 * @property testRootPaths test suite paths
 */
data class TestSuitesRepo(
    val organizationName: String,
    val url: String,
    val branch: String,
    val testRootPaths: List<String>,
)
