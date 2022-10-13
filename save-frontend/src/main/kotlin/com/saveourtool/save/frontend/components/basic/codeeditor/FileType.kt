package com.saveourtool.save.frontend.components.basic.codeeditor

import com.saveourtool.save.frontend.externals.reactace.AceModes

private val codeExample = """
            |package com.example
            |
            |data class BestLanguage(val name = "Kotlin")
            |
            |fun main {
            |    val bestLanguage = BestLanguage()
            |    println("saveourtool loves ${'$'}{bestLanguage.name}")
            |}
        """.trimMargin()

private val configExample = """
            |[general]
            |tags = ["demo"]
            |description = "saveourtool online demo"
            |suiteName = "Test"
            |execCmd="RUN_COMMAND"
            |language = "Kotlin"
            |
            |[warn]
            |execFlags = "--build-upon-default-config -i"
            |actualWarningsPattern = "\\w+ - (\\d+)/(\\d+) - (.*)${'$'}" # (default value)
            |testNameRegex = ".*Test.*" # (default value)
            |patternForRegexInWarning = ["{{", "}}"]
            |# Extra flags will be extracted from a line that matches this regex if it's present in a file
            |runConfigPattern = "# RUN: (.+)"
        """.trimMargin()

private val setupShExample = """
            |# Here you can add some additional commands required to run your tool e.g.
            |# python -m pip install pylint
        """.trimMargin()

/**
 * @property prettyName displayed name
 * @property editorMode highlight mode that should be enabled, if null, mode can be chosen using selector
 * @property textExample example of file content
 */
enum class FileType(val prettyName: String, val editorMode: AceModes?, val textExample: String) {
    CODE("code", null, codeExample),
    SAVE_TOML("save.toml", AceModes.TOML, configExample),
    SETUP_SH("setup.sh", AceModes.SHELL, setupShExample),
    ;
    companion object {
        /**
         * @param fileType [FileType]
         * @param optionForCode will be returned if [fileType] is CODE
         * @param optionForSaveToml will be returned if [fileType] is SAVE_TOML
         * @param optionForSetupSh will be returned if [fileType] is SETUP_SH
         * @return option corresponding to [fileType]
         */
        fun <T> getTypedOption(
            fileType: FileType,
            optionForCode: T,
            optionForSaveToml: T,
            optionForSetupSh: T,
        ) = when (fileType) {
            CODE -> optionForCode
            SAVE_TOML -> optionForSaveToml
            SETUP_SH -> optionForSetupSh
        }

        /**
         * @param fileType [FileType]
         * @param optionForCode will be returned if [fileType] is CODE
         * @param optionForSaveToml will be returned if [fileType] is SAVE_TOML
         * @param optionForSetupSh will be returned if [fileType] is SETUP_SH
         * @return option corresponding to [fileType] or null if [fileType] is null
         */
        fun <T> getTypedOption(
            fileType: FileType?,
            optionForCode: T,
            optionForSaveToml: T,
            optionForSetupSh: T,
        ) = fileType?.let { getTypedOption(it, optionForCode, optionForSaveToml, optionForSetupSh) }
    }
}
