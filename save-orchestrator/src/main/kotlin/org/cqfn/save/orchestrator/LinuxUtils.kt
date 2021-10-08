/**
 * Utilities for working with Linux
 */

package org.cqfn.save.orchestrator

/**
 * @param hostname hostname to be resolved via `hosts` file
 * @return IP address of [hostname] if it has been found
 * @throws IllegalArgumentException if the IP from `hosts` doesn't match pattern for IP address
 */
fun getHostIp(hostname: String): String? {
    val processes = ProcessBuilder.startPipeline(
        listOf(
            ProcessBuilder().command("getent", "hosts", hostname),
            ProcessBuilder().command("awk", "'{print \$1}'")
        )
    )
    val hostIp: String? = processes.last()
        .inputStream
        .bufferedReader()
        .readLine()
    require(hostIp == null || hostIp.matches(Regex("(\\d+\\.){3}\\d+"))) {
        "Discovered IP [$hostIp] is not valid"
    }
    return hostIp
}
