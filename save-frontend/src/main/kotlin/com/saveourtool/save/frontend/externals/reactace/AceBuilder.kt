@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.externals.reactace

import com.saveourtool.save.utils.DEBOUNCE_PERIOD_FOR_EDITORS
import com.saveourtool.save.utils.Languages

import io.github.petertrr.diffutils.diff
import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import web.cssom.ClassName

typealias AceMarkers = Array<AceMarker>

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
    selectedMode: Languages,
    selectedTheme: AceThemes = AceThemes.CHROME,
    aceMarkers: Array<AceMarker> = emptyArray(),
    disabled: Boolean = false,
    onChangeFun: (String) -> Unit,
) {
    selectedTheme.require()
    kotlinext.js.require("ace-builds/src-min-noconflict/mode-${selectedMode.modeName}")

    div {
        className = ClassName("d-flex justify-content-center flex-fill")
        reactAce {
            fontSize = "16px"
            className = "flex-fill"
            mode = selectedMode.modeName
            theme = selectedTheme.themeName
            width = "auto"
            debounceChangePeriod = DEBOUNCE_PERIOD_FOR_EDITORS
            value = text
            showPrintMargin = false
            readOnly = disabled
            onChange = { value, _ -> onChangeFun(value) }
            markers = aceMarkers
        }
    }
}

/**
 * Get array of [AceMarker]s for modified lines of a [String].
 *
 * @param oldString old version of string
 * @param newString new version of string
 * @return Array of [AceMarker]s corresponding to modified lines.
 */
fun getAceMarkers(oldString: String, newString: String) = diff(
    oldString.split("\n"),
    newString.split("\n"),
)
    .deltas
    .map {
        it.target.position to it.target.last()
    }
    .map { (from, to) ->
        aceMarkerBuilder(from, to)
    }
    .toTypedArray()

/**
 * Get [AceMarker]
 *
 * @param beginLineIndex index of the first line to be marked
 * @param endLineIndex index of the last line to be marked
 * @param markerType type of marker
 * @param classes
 * @return [AceMarker]
 */
fun aceMarkerBuilder(
    beginLineIndex: Int,
    endLineIndex: Int = beginLineIndex,
    markerType: String = "fullLine",
    classes: String = "unsaved-marker",
): AceMarker = jso {
    startRow = beginLineIndex
    endRow = endLineIndex
    startCol = 0
    endCol = 1
    className = classes
    type = markerType
    inFront = false
}

/**
 * Get [AceMarker] by [positionString] - string in format
 *
 * `<START_LINE>:<START_CHAR>-<END_LINE>:<END_CHAR>`
 *
 * @param positionString string that defines start and end positions
 * @param classes
 * @return [AceMarker]
 */
fun aceMarkerBuilder(
    positionString: String?,
    classes: String = "unsaved-marker",
): AceMarker? = positionString?.split("-")?.let { (start, end) ->
    val (startRowStr, startColStr) = start.split(":")
    val (endRowStr, endColStr) = end.split(":")
    jso {
        startRow = startRowStr.toInt()
        startCol = startColStr.toInt()
        endRow = endRowStr.toInt()
        endCol = endColStr.toInt()
        className = classes
        inFront = false
    }
}

/**
 * Get [AceMarkers] by [CpgNodeAdditionalParams.location] - string in format
 *
 * `<FILENAME>(<START_LINE>:<START_CHAR>-<END_LINE>:<END_CHAR>)`
 *
 * @param location string that contains start and end positions
 * @return [AceMarker]
 */
fun getAceMarkers(location: String?): AceMarkers = location?.substringAfter("(")
    ?.substringBefore(")")
    .takeIf { it != location }
    .let { positionString ->
        aceMarkerBuilder(positionString)?.let { arrayOf(it) } ?: emptyArray()
    }
