@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.frontend.PlatformLanguages
import com.saveourtool.save.frontend.externals.i18next.useTranslation
import js.core.jso
import react.FC
import react.VFC
import react.dom.aria.AriaHasPopup
import react.dom.aria.ariaExpanded
import react.dom.aria.ariaHasPopup
import react.dom.aria.ariaLabelledBy
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span
import react.useEffect
import react.useState
import web.cssom.ClassName
import web.cssom.Cursor

private const val LANG_DROPDOWN_ID = "lang-dropdown"

/**
 * A [FC] that is responsible for language selection
 */
val languageSelector: VFC = FC {
    val (_, i18n) = useTranslation()
    val (language, setSelectedLanguage) = useState(PlatformLanguages.defaultLanguage)
    useEffect(language) { i18n.changeLanguage(language.code) }

    div {
        className = ClassName("dropdown")
        a {
            className = ClassName("dropdown-toggle")
            id = LANG_DROPDOWN_ID
            asDynamic()["data-toggle"] = "dropdown"
            ariaHasPopup = true.unsafeCast<AriaHasPopup>()
            ariaExpanded = false
            style = jso { cursor = "pointer".unsafeCast<Cursor>() }
            span { +language.label }
        }

        div {
            className = ClassName("dropdown-menu")
            ariaLabelledBy = LANG_DROPDOWN_ID
            PlatformLanguages.values().map { language ->
                a {
                    className = ClassName("dropdown-item")
                    onClick = { setSelectedLanguage(language) }
                    span { +language.label }
                }
            }
        }
    }
}
