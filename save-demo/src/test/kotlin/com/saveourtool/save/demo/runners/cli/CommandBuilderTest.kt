package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.DemoMode
import com.saveourtool.save.demo.runners.command.CommandBuilder
import com.saveourtool.save.demo.runners.command.CommandContext
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CommandBuilderTest {
    private val builder = CommandBuilder()

    @Test
    fun testDiktat() {
        val commandContext = CommandContext(
            "./test.kt".toPath().toNioPath(),
            mapOf(
                "ktlint" to "ktlint".toPath().toNioPath(),
                "diktat" to "diktat-1.2.3.jar".toPath().toNioPath(),
            ),
            "./output.txt".toPath().toNioPath(),
            null,
        )
        @Suppress("MaxLineLength")
        Assertions.assertEquals(
            "ktlint -R diktat-1.2.3.jar --disabled_rules=diktat-ruleset:package-naming,standard --reporter=plain,output=output.txt",
            builder.build("\${tools.ktlint} -R \${tools.diktat} --disabled_rules=diktat-ruleset:package-naming,standard --reporter=plain,output=\${outputPath}", commandContext)
        )
    }
}
