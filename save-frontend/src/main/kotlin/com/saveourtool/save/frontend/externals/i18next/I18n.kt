package com.saveourtool.save.frontend.externals.i18next

/**
 * Class that represents i18n object
 */
external class I18n {
    /**
     * Current language
     */
    val language: String

    /**
     * Set language by language code
     *
     * @param language language code
     *
     * @see I18n.changeLanguage
     */
    fun changeLanguage(language: String)
}
