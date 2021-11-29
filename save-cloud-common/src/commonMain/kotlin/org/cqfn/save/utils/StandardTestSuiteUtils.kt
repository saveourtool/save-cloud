@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.utils

/**
 * Directory in docker image, where all requested standard test suites will be located for execution
 */
const val STANDARD_TEST_SUITE_DIR = "standard-test-suites"

/**
 * Since in standard mode we create additional directories for correct execution,
 * we will also mark them with such prefix, in aim to correctly match test execution results with data from DB,
 * which didn't know about our actions with creation of additional dirs
 */
const val PREFIX_FOR_SUITES_LOCATION_IN_STANDARD_MODE = "STANDARD_"
