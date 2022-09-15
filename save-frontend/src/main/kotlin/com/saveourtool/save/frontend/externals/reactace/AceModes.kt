package com.saveourtool.save.frontend.externals.reactace

/**
 * Highlight modes for AceEditor
 *
 * @property modeName substring of file with required highlight mode
 */
enum class AceModes(val modeName: String) {
    /**
     * C/C++ highlight mode
     */
    CPP("c_cpp"),

    /**
     * Golang highlight mode
     */
    GO("golang"),

    /**
     * Java highlight mode
     */
    JAVA("java"),

    /**
     * Javascript highlight mode
     */
    JAVASCRIPT("jsx"),

    /**
     * Kotlin highlight mode
     */
    KOTLIN("kotlin"),

    /**
     * Toml highlight mode
     */
    TOML("toml"),

    /**
     * Typescript highlight mode
     */
    TYPESCRIPT("typescript"),
    ;

    /**
     * Method that includes required highlight mode
     */
    fun require() = kotlinext.js.require("ace-builds/src-min-noconflict/mode-$modeName")
}
