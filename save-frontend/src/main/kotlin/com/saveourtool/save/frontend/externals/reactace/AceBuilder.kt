@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.externals.reactace

import com.saveourtool.save.utils.DEBOUNCE_PERIOD_FOR_EDITORS
import csstype.ClassName
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div

/**
 * @param text displayed text
 * @param selectedMode highlight mode
 * @param selectedTheme displayed theme
 * @param aceMarkers array of [AceMarker]s that defines which lines should be marked as unsaved
 * @param disabled should this editor be readonly
 * @param onChangeFun callback invoked on input
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
fun ChildrenBuilder.aceBuilder(
    text: String,
    selectedMode: AceModes,
    selectedTheme: AceThemes = AceThemes.CHROME,
    aceMarkers: Array<AceMarker> = emptyArray(),
    disabled: Boolean = false,
    onChangeFun: (String) -> Unit,
) {
    selectedTheme.require()
    selectedMode.require()
    div {
        className = ClassName("d-flex justify-content-center flex-fill")
        reactAce {
            className = "flex-fill"
            mode = selectedMode.modeName
            theme = selectedTheme.themeName
            width = "auto"
            debounceChangePeriod = DEBOUNCE_PERIOD_FOR_EDITORS
            value = text
            showPrintMargin = false
            readOnly = disabled
            onChange = { value, _ ->
                onChangeFun(value)
            }
            markers = aceMarkers
        }
    }
}
