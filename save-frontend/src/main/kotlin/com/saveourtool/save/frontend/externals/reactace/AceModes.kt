package com.saveourtool.save.frontend.externals.reactace

/**
 * Highlight modes for AceEditor
 *
 * @property modeName substring of file with required highlight mode
 * @property prettyName
 */
@Suppress("IDENTIFIER_LENGTH")
enum class AceModes(val modeName: String, val prettyName: String) {
    /**
     * C highlight mode
     */
    C("c_cpp", "C"),

    /**
     * C++ highlight mode
     */
    CPP("c_cpp", "C++"),

    /**
     * Golang highlight mode
     */
    GO("golang", "GO"),

    /**
     * Java highlight mode
     */
    JAVA("java", "JAVA"),

    /**
     * Javascript highlight mode
     */
    JAVASCRIPT("jsx", "JS"),

    /**
     * Kotlin highlight mode
     */
    KOTLIN("kotlin", "KOTLIN"),

    /**
     * Python highlight mode
     */
    PYTHON("python", "PYTHON"),

    /**
     * Shell highlight mode
     */
    SHELL("sh", "SHELL"),

    /**
     * Toml highlight mode
     */
    TOML("toml", "TOML"),

    /**
     * Typescript highlight mode
     */
    TYPESCRIPT("typescript", "TYPESCRIPT"),
    ;

    /**
     * Method that includes required highlight mode
     */
    fun require() = kotlinext.js.require("ace-builds/src-min-noconflict/mode-$modeName")
}
