@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.externals.reactace

import react.ChildrenBuilder

/**
 * @param text
 * @param selectedMode
 * @param selectedTheme
 * @param onChangeFun
 */
fun ChildrenBuilder.aceBuilder(
    text: String,
    selectedMode: AceModes,
    selectedTheme: AceThemes = AceThemes.CHROME,
    onChangeFun: (String) -> Unit,
) {
    selectedTheme.require()
    selectedMode.require()
    reactAce {
        value = text
        onChange = { value, _ ->
            onChangeFun(value)
        }
        mode = selectedMode.modeName
        theme = selectedTheme.themeName
    }
}
