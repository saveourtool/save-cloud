/**
 * Utility functions for System
 */

package com.saveourtool.save.demo.utils

private fun String.isWindows() = startsWith("Windows", ignoreCase = true)

/**
 * @return true if current system is Windows, false otherwise
 */
fun isWindows() = System.getProperty("os.name").isWindows()
