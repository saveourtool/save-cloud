@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.testsuite.TestSuiteDto

/**
 * Create a piece of HTML with formatted representation of test suite
 *
 * @param suite a test suite to display
 * @return a string containing HTML
 */
fun suiteDescription(suite: TestSuiteDto) =
        """
            <div>
                  <ul class="pl-3">
                  ${suite.source.gitDto.url?.let {
            "<li><a href=$it>$it</a></li>"
        }}
                <li>${suite.source.testRootPath}</li>
                </ul>
                <p>${suite.description ?: ""}</p>
            </div>
        """.trimIndent()
