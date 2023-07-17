/**
 * Utilities for cli args parsing
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.apicli

import com.saveourtool.save.api.authorization.Authorization
import com.saveourtool.save.execution.TestingType

import org.slf4j.LoggerFactory

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

private val log = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

/**
 * @property authorization authorization data
 * @property mode mode of execution (one of [TestingType])
 * @property contestName name of the contest in which the tool participates
 */
data class CliArguments(
    val authorization: Authorization,
    val mode: TestingType,
    val contestName: String? = null,
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
        .required()

    val mode by parser.option(
        ArgType.Choice<TestingType>(),
        fullName = "mode",
        shortName = "m",
        description = "Mode of execution: git/standard"
    )
        .required()

    val contestName by parser.option(
        ArgType.String,
        fullName = "contest-name",
        shortName = "cn",
        description = "Name of the contest that this tool participates in",
    )

    parser.parse(args)

    // FIXME
    val authorization = oauth2Source?.let {
        Authorization(username!!, it, token)
    } ?: Authorization(username!!, "basic", token)

    return CliArguments(
        authorization,
        mode,
        contestName,
    )
}
