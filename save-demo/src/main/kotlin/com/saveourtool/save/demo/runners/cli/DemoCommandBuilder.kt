package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.DemoMode
import org.springframework.expression.Expression
import org.springframework.expression.spel.standard.SpelExpressionParser
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class DemoCommandBuilder {
    private val expressionParser = SpelExpressionParser()
    private val expressionCache = ConcurrentHashMap<String, Expression>()

    fun build(templateCommand: String, context: Context): String {
        val expression = expressionCache.getOrPut(templateCommand) {
            expressionParser.parseExpression(templateCommand)
        }
        return expression.getValue(context, String::class.java) ?: error("failed")
    }

    data class Context(
        val testPath: Path,
        val tools: Map<String, Path>,
        val outputPath: Path,
        val mode: DemoMode,
    )
}