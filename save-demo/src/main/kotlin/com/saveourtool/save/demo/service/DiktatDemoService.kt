package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.diktat.DiktatDemoAdditionalParams
import com.saveourtool.save.demo.diktat.DiktatDemoMode
import com.saveourtool.save.demo.diktat.DiktatDemoResult
import com.saveourtool.save.demo.diktat.DiktatDemoTool

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import org.cqfn.diktat.ruleset.rules.DiktatRuleSetProvider
import org.springframework.stereotype.Service

import java.io.File
import java.util.*

import kotlin.collections.ArrayList

/**
 * Demo service implementation for ktlint-demo/diktat-demo
 */
@Service
class DiktatDemoService : AbstractDemoService<DiktatDemoAdditionalParams, DiktatDemoResult> {
    private fun generateFileName() = UUID.randomUUID().toString()
    private fun generateDemoFile(generatedName: String): File = File("demo-input-$generatedName")
    private fun generateDemoConfig(generatedName: String): File = File("demo-config-$generatedName")

    /**
     * @param demoFileLines kotlin file to be checked
     * @param demoAdditionalParams instance of [DiktatDemoAdditionalParams]
     */
    override fun runDemo(demoFileLines: List<String>, demoAdditionalParams: DiktatDemoAdditionalParams?): DiktatDemoResult {
        val fileName = generateFileName()

        val tool = demoAdditionalParams?.tool ?: DiktatDemoTool.DIKTAT
        val demoMode = demoAdditionalParams?.mode ?: DiktatDemoMode.WARN
        val demoConfigLines = demoAdditionalParams?.config

        val demoFile = prepareDemoFile(demoFileLines.joinToString("\n"), fileName)
        val demoConfig = prepareDemoConfig(demoConfigLines, fileName)

        return try {
            processDemo(tool, demoMode, demoConfig, demoFile)
        } finally {
            deleteTempFiles(listOf(demoFile, demoConfig))
        }
    }

    private fun processDemo(
        tool: DiktatDemoTool,
        demoMode: DiktatDemoMode,
        demoConfig: File?,
        demoFile: File
    ): DiktatDemoResult {
        val ruleSets = when (tool) {
            DiktatDemoTool.DIKTAT -> getDiktatRuleSets(demoConfig)
            DiktatDemoTool.KTLINT -> getKtLintRuleSets()
            else -> throw IllegalStateException("Unknown ruleset was requested.")
        }

        return when (demoMode) {
            // DiktatDemoMode.FIX -> runFixDemo(demoFile, ruleSets)
            DiktatDemoMode.WARN -> runCheckDemo(demoFile, ruleSets)
            else -> throw IllegalStateException("Unknown demoMode was requested.")
        }
    }

    private fun getDiktatRuleSets(config: File?) = listOf(
        config?.let {
            DiktatRuleSetProvider(it.absolutePath).get()
        } ?: DiktatRuleSetProvider().get()
    )

    private fun getKtLintRuleSets() = listOf(StandardRuleSetProvider().get())

    private fun runFixDemo(demoFile: File, ruleSets: Iterable<RuleSet>): DiktatDemoResult {
        val warnings: ArrayList<LintError> = ArrayList()
        return KtLint.format(
            KtLint.ExperimentalParams(
                fileName = demoFile.absolutePath,
                text = demoFile.readText(),
                ruleSets = ruleSets,
                cb = { lintError, _ -> warnings.add(lintError) }
            )
        )
            .let {
                DiktatDemoResult(warnings.toListOfStrings(), it)
            }
    }

    private fun runCheckDemo(demoFile: File, ruleSets: Iterable<RuleSet>): DiktatDemoResult {
        val warnings: ArrayList<LintError> = ArrayList()
        val inputText = demoFile.readText()
        KtLint.lint(
            KtLint.ExperimentalParams(
                fileName = demoFile.absolutePath,
                text = inputText,
                ruleSets = ruleSets,
                cb = { lintError, _ -> warnings.add(lintError) }
            )
        )
        return DiktatDemoResult(warnings.toListOfStrings(), inputText)
    }

    private fun prepareDemoConfig(configLines: String?, generatedName: String) = configLines?.let {
        generateDemoConfig(generatedName)
    }
        ?.apply {
            writeText(configLines)
        }

    private fun prepareDemoFile(fileLines: String, generatedName: String) = generateDemoFile(generatedName)
        .apply {
            writeText(fileLines)
        }

    private fun deleteTempFiles(files: List<File?>) = files.forEach {
        it?.delete()
    }

    private fun ArrayList<LintError>.toListOfStrings() = map { "(${it.line}): ${it.detail}" }
}
