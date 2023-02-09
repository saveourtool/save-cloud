package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.DemoMode
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DemoCommandBuilderTest {
    private val builder = DemoCommandBuilder()

    @Test
    fun testDiktat() {
        val context = DemoCommandBuilder.Context(
            "./test.kt".toPath().toNioPath(),
            mapOf(
                "ktlint" to "ktlint".toPath().toNioPath(),
                "diktat" to "diktat-1.2.3.jar".toPath().toNioPath(),
            ),
            "./output.txt".toPath().toNioPath(),
            DemoMode.FIX,
        )
        Assertions.assertEquals(
            "",
            builder.build("tools['ktlint'] + ' -R '+ tools['diktat'] + ' --disabled_rules=diktat-ruleset:package-naming,standard --reporter=plain,output=' + outputPath + mode == 'FIX' ? '--format' : ''", context)
        )
    }
}