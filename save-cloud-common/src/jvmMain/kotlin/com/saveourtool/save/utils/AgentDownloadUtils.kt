@file:Suppress("FILE_NAME_MATCH_CLASS")
/**
 * Utils for building curl command that downloads save-agent and save-demo-agent to pod
 */

package com.saveourtool.save.utils

import org.intellij.lang.annotations.Language

@Language("bash")
private val defaultShellOptions: Sequence<String> = sequenceOf (
    "errexit",
    "nounset",
    "xtrace",
)

@Language("bash")
private val defaultCurlOptions: Sequence<String> = sequenceOf(
    "-vvv",
    "--fail",
)

/**
 * @property executableName
 */
enum class AgentType(val executableName: String) {
    AGENT("save-agent.kexe"),
    DEMO_AGENT("save-demo-agent.kexe"),
    ;
}

/**
 * Get command for newly created pod that would download agent of type [agentType] from [downloadUrl]
 * using curl with [curlOptions], chmod downloaded executable and launch it.
 *
 * Default options for shell ([defaultShellOptions]):
 * - `set -e` | `set -o errexit`: exit immediately if any command has a non-zero status;
 * - `set -u` | `set -o nounset`: exit immediately if a referenced variable is undefined;
 * - `set -x` | `set -o xtrace`: enable debugging (PS4 followed by command & args).
 *
 * Default options for curl ([defaultCurlOptions]):
 * - `--fail` is necessary so that `curl` exits immediately upon an HTTP 404;
 * - `-vvv` logging level.
 *
 * @param downloadUrl url to download agent
 * @param agentType type of agent to download: either [AgentType.AGENT] or [AgentType.DEMO_AGENT]
 * @param shellOptions options to be set on command execution, [defaultShellOptions] by default
 * @param curlOptions options to be passed to curl, [defaultCurlOptions] by default
 * @return run command
 */
fun downloadAndRunAgentCommand(
    downloadUrl: String,
    agentType: AgentType,
    shellOptions: Sequence<String> = defaultShellOptions,
    curlOptions: Sequence<String> = defaultCurlOptions,
): String = with(agentType) {
    "set ${getShellOptions(shellOptions)}" +
            " && curl ${getCurlOptions(curlOptions)} $downloadUrl --output $executableName" +
            " && chmod +x $executableName" +
            " && ./$executableName"
}

/**
 * @return [shellOptions] as a single string.
 * @see shellOptions
 */
@Language("bash")
private fun getShellOptions(shellOptions: Sequence<String>): String =
        shellOptions.map { option ->
            "-o $option"
        }.joinToString(separator = " ")

/**
 * @return [curlOptions] as a single string.
 * @see curlOptions
 */
@Language("bash")
private fun getCurlOptions(curlOptions: Sequence<String>): String = curlOptions.joinToString(separator = " ")
