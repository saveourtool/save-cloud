package org.cqfn.save.api

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import org.cqfn.save.execution.ExecutionType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

data class CliArguments(
    val authorization: Authorization,
    val mode: ExecutionType,
)

fun parseArguments(args: Array<String>): CliArguments? {
    if (args.isEmpty()) {
        log.error("Argument list couldn't be empty!")
        return null
    }
    val parser = ArgParser("")

    val userName by parser.option(
        ArgType.String,
        fullName = "user",
        shortName = "u",
        description = "User name in SAVE-cloud system"
    )

    // FixMe: any opportunity to hide process of password entering, via some additional window which doesn't show user input?
    val password by parser.option(
        ArgType.String,
        fullName = "password",
        shortName = "p",
        description = "Password"
    )

    val mode by parser.option(
        ArgType.Choice<ExecutionType>(),
        fullName = "mode",
        shortName = "m",
        description = "Mode of execution GIT/STANDARD"
    )
    parser.parse(args)
    return CliArguments(
        Authorization(userName!!, password),
        mode!!
    )
}
