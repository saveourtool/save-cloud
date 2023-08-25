package com.saveourtool.save.frontend.externals.i18next

/**
 * Class that represents the return value of `useTranslation` hook
 * @see useTranslation
 */
@Suppress("NOTHING_TO_INLINE")
sealed class Translation {
    /**
     * @return t-function that receives a key and returns a localized value
     */
    inline operator fun component1(): (String) -> String = asDynamic()[0].unsafeCast<(String) -> String>()

    /**
     * Get an i18n instance and use
     *
     * ```
     *   i18n.changeLanguage("LANG")
     * ```
     *
     * in order to change language
     *
     * @return an i18n instance
     */
    inline operator fun component2(): dynamic = asDynamic()[1]

    /**
     * @return ready flag
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    inline operator fun component3(): Boolean = asDynamic()[2].unsafeCast<Boolean>()

    /**
     * Operator that should be used in order to get rid of this:
     *
     * ```
     *   val (t) = useTranslation()
     * ```
     * and use this:
     *
     * ```
     *   val t = useTranslation()
     * ```
     *
     * @param key key for translation
     * @return localized value by [key]
     * @see component1
     */
    inline operator fun invoke(key: String): String = component1()(key)
}
