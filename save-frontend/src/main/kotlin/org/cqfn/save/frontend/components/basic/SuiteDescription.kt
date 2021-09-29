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
              ${suite.testSuiteRepoUrl?.let {
            "<a href=$it>$it</a><br/>"
        }}
              <p>${suite.description ?: ""}</p>
            </div>
        """.trimIndent()
