/**
 * Utility functions for System
 */

package com.saveourtool.save.buildutils

private fun String.isWindows() = startsWith("Windows", ignoreCase = true)

private fun String.isMac() = startsWith("Mac", ignoreCase = true)

private fun String.isLinux() = startsWith("Linux", ignoreCase = true)

/**
 * @return true if current system is Windows, false otherwise
 */
fun isWindows() = System.getProperty("os.name").isWindows()

/**
 * @return true if current system is macOS, false otherwise
 */
fun isMac() = System.getProperty("os.name").isMac()

/**
 * @return true if current system is linux, false otherwise
 */
fun isLinux() = System.getProperty("os.name").isLinux()
