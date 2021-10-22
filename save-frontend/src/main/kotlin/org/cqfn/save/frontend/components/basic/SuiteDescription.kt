@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.testsuite.TestSuiteDto

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
                  ${suite.testSuiteRepoUrl?.let {
            "<li><a href=$it>$it</a></li>"
        }}
                <li>${suite.testRootPath}</li>
                </ul>
                <p>${suite.description ?: ""}</p>
            </div>
        """.trimIndent()
