@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic.codeeditor

import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.externals.reactace.AceMarker
import com.saveourtool.save.frontend.externals.reactace.AceModes
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.externals.reactace.aceBuilder
import com.saveourtool.save.frontend.utils.*

import csstype.ClassName
import react.ChildrenBuilder
import react.FC
import react.Props
import react.useState
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6

import kotlinx.js.jso

/**
 * CodeEditor component
 */
val codeEditorComponent = codeEditorComponent()

private val toolbarCard = cardComponent(isBordered = true)

/**
 * CodeEditor functional component [Props]
 */
external interface CodeEditorComponentProps : Props {
    /**
     * Title of an editor
     */
    var editorTitle: String

    /**
     * Currently inputted text
     */
    var editorText: String

    /**
     * Text saved on backend
     */
    var savedEditorText: String

    /**
     * Currently selected [FileType]
     */
    var selectedFile: FileType?

    /**
     * Callback invoked for selectFile change
     */
    var onSelectedFileUpdate: (FileType?) -> Unit

    /**
     * Callback invoked on ace editor change
     */
    var onEditorTextUpdate: (String) -> Unit

    /**
     * Action to save changes
     */
    var doUploadChanges: () -> Unit

    /**
     * Action to reload changes
     */
    var doReloadChanges: () -> Unit

    /**
     * Action to run execution
     */
    var doRunExecution: () -> Unit

    /**
     * Action to reload debug info
     */
    var doResultReload: () -> Unit
}

/**
 * @property prettyName displayed name
 * @property editorMode highlight mode that should be enabled, if null, mode can be chosen using selector
 */
enum class FileType(val prettyName: String, val editorMode: AceModes?) {
    CODE("code", null),
    SAVE_TOML("save.toml", AceModes.TOML),
    SETUP_SH("setup.sh", AceModes.SHELL),
    ;
}

@Suppress(
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
    "TOO_LONG_FUNCTION",
    "LongMethod",
)
private fun ChildrenBuilder.displayEditorToolbar(
    selectedMode: AceModes,
    selectedTheme: AceThemes,
    selectedFileType: FileType?,
    setSelectedMode: (String) -> Unit,
    setSelectedTheme: (String) -> Unit,
    onUploadChanges: () -> Unit,
    onReloadChanges: () -> Unit,
    onRunExecution: () -> Unit,
    onResultReload: () -> Unit,
    onFileTypeChange: (FileType) -> Unit,
) {
    toolbarCard {
        div {
            className = ClassName("input-group")
            div {
                className = ClassName("input-group-prepend")

                button {
                    type = ButtonType.button
                    className = ClassName("btn btn-outline-primary")
                    onClick = onUploadChanges.withUnusedArg()
                    fontAwesomeIcon(icon = faUpload)
                    asDynamic()["data-toggle"] = "tooltip"
                    asDynamic()["data-placement"] = "top"
                    title = "Save changes on server"
                }
                button {
                    type = ButtonType.button
                    className = ClassName("btn btn-outline-primary")
                    onClick = onReloadChanges.withUnusedArg()
                    fontAwesomeIcon(icon = faDownload)
                    asDynamic()["data-toggle"] = "tooltip"
                    asDynamic()["data-placement"] = "top"
                    title = "Load changes from server"
                }
                FileType.values().forEach { fileType ->
                    buttonBuilder(
                        fileType.prettyName,
                        "primary",
                        isOutline = true,
                        isActive = selectedFileType == fileType,
                    ) {
                        onFileTypeChange(fileType)
                    }
                }
            }
            selectorBuilder(
                selectedFileType?.editorMode?.modeName ?: selectedMode.modeName,
                AceModes.values().map { it.modeName },
                "custom-select",
                selectedFileType?.editorMode != null,
            ) { event ->
                setSelectedMode(event.target.value)
            }
            selectorBuilder(
                selectedTheme.themeName,
                AceThemes.values().map { it.themeName },
                "custom-select",
                false,
            ) { event ->
                setSelectedTheme(event.target.value)
            }
            div {
                className = ClassName("input-group-append")

                button {
                    type = ButtonType.button
                    className = ClassName("btn btn-outline-info")
                    onClick = onResultReload.withUnusedArg()
                    fontAwesomeIcon(icon = faReload)
                    asDynamic()["data-toggle"] = "tooltip"
                    asDynamic()["data-placement"] = "top"
                    title = "Fetch debug info"
                }
                button {
                    type = ButtonType.button
                    className = ClassName("btn btn-outline-success")
                    onClick = onRunExecution.withUnusedArg()
                    fontAwesomeIcon(icon = faCaretSquareRight)
                    asDynamic()["data-toggle"] = "tooltip"
                    asDynamic()["data-placement"] = "top"
                    title = "Run execution"
                }
            }
        }
    }
}

private fun codeEditorComponent() = FC<CodeEditorComponentProps> { props ->
    val (selectedMode, setSelectedMode) = useState(AceModes.KOTLIN)
    val (selectedTheme, setSelectedTheme) = useState(AceThemes.CHROME)
    val firstLoader = useOnceAction()
    div {
        h6 {
            className = ClassName("text-center text-primary")
            +props.editorTitle
        }
        displayEditorToolbar(
            selectedMode,
            selectedTheme,
            props.selectedFile,
            { newModeName ->
                setSelectedMode(AceModes.values().find { it.modeName == newModeName }!!)
            },
            { newThemeName ->
                setSelectedTheme(AceThemes.values().find { it.themeName == newThemeName }!!)
            },
            onUploadChanges = props.doUploadChanges,
            onReloadChanges = props.doReloadChanges,
            onRunExecution = props.doRunExecution,
            onResultReload = props.doResultReload,
        ) { fileType ->
            firstLoader {
                props.doReloadChanges()
            }
            props.selectedFile?.run {
                props.doUploadChanges()
            }
            if (fileType == props.selectedFile) {
                props.onSelectedFileUpdate(null)
            } else {
                props.onSelectedFileUpdate(fileType)
            }
        }

        aceBuilder(
            props.editorText,
            selectedMode,
            selectedTheme,
            getAceMarkers(props.savedEditorText, props.editorText),
            props.selectedFile == null,
            props.onEditorTextUpdate,
        )
    }
}

private fun getAceMarkers(oldFile: String, newFile: String): Array<AceMarker> = getChangedLinesIndices(oldFile, newFile)
    .map { getAceMarker(it) }
    .toTypedArray()

private fun getAceMarker(lineIndex: Int): AceMarker = jso {
    startRow = lineIndex
    endRow = lineIndex
    startCol = 0
    endCol = 1
    className = "unsaved-marker"
    type = "fullLine"
    inFront = false
}

private fun getChangedLinesIndices(oldFile: String, newFile: String): Set<Int> {
    val oldLines = oldFile.split("\n").map { "$it\n" }
    val newLines = newFile.split("\n").map { "$it\n" }

    val extendedOldLines: List<String> = oldLines.extendWithEmptyStrings(maxOf(oldLines.size, newLines.size))
    val extendedNewLines: List<String> = newLines.extendWithEmptyStrings(maxOf(oldLines.size, newLines.size))

    return extendedOldLines.zip(extendedNewLines)
        .mapIndexed { index, (oldLine, newLine) ->
            Triple(index, oldLine, newLine)
        }
        .filter { (_, oldLine, newLine) ->
            oldLine != newLine
        }
        .map { (index, _, _) ->
            index
        }
        .toSet()
}

private fun List<String>.extendWithEmptyStrings(requiredSize: Int) = plus(List(requiredSize - size) { "" })
