/**
 * Utilities for working with Linux
 */

package org.cqfn.save.orchestrator

/**
 * @param hostname hostname to be resolved via `hosts` file
 * @return IP address of [hostname] if it has been found or null
 */
fun getHostIp(hostname: String): String? {
    val process = ProcessBuilder(
        "bash", "-c",
        "getent hosts $hostname | awk '{print \$1}'"
    )
        .start()
    process.waitFor()
    return process.inputStream
        .readAllBytes()
        .decodeToString()
        .lines()
        .firstOrNull()
        ?.takeIf { it.isNotBlank() }
}
