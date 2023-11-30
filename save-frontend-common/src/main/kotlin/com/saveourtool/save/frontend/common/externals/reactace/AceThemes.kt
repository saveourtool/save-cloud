package com.saveourtool.save.frontend.common.externals.reactace

/**
 * Themes for AceEditor
 *
 * @property themeName substring of file with required theme
 */
enum class AceThemes(val themeName: String) {
    /**
     * Light theme
     */
    CHROME("chrome"),

    /**
     * Another light theme, but worse
     */
    GITHUB("github"),

    /**
     * Nice dark theme
     */
    MONOKAI("monokai"),
    ;

    /**
     * Method that includes required theme
     */
    fun require() = kotlinext.js.require<dynamic>("ace-builds/src-min-noconflict/theme-$themeName")
    companion object {
        /**
         * Theme that is recommended to be used everywhere
         */
        val preferredTheme = CHROME
    }
}
