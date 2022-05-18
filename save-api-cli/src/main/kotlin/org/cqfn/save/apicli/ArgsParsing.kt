/**
 * Utilities for cli args parsing
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.apicli

import org.cqfn.save.api.authorization.Authorization
import org.cqfn.save.execution.ExecutionType

import org.slf4j.LoggerFactory

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType

private val log = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

/**
 * @property authorization authorization data
 * @property mode mode of execution: git/standard
 */
data class CliArguments(
    val authorization: Authorization,
    val mode: ExecutionType,
)

/**
 * @param args
 * @return parsed command line arguments
 */
@Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
fun parseArguments(args: Array<String>): CliArguments? {
    if (args.isEmpty()) {
        log.error("Argument list couldn't be empty!")
        return null
    }
    val parser = ArgParser("")

    val username by parser.option(
        ArgType.String,
        fullName = "user",
        shortName = "u",
        description = "User name in SAVE-cloud system"
    )

    val oauth2Source by parser.option(
        ArgType.String,
        fullName = "oauth2Source",
        shortName = "o",
        description = "Oauth2 source, where the user identity is coming from"
    )

    // FixMe: any opportunity to hide process of password entering, via some additional window which doesn't show user input?
    val token by parser.option(
        ArgType.String,
        fullName = "token",
        shortName = "t",
        description = "OAuth token for SAVE-cloud system"
    )

    val mode by parser.option(
        ArgType.Choice<ExecutionType>(),
        fullName = "mode",
        shortName = "m",
        description = "Mode of execution: git/standard"
    )
    parser.parse(args)

    val authorization = oauth2Source?.let {
        Authorization("$oauth2Source@${username!!}", token)
    } ?: Authorization(username!!, token)

    return CliArguments(
        authorization,
        mode!!
    )
}
