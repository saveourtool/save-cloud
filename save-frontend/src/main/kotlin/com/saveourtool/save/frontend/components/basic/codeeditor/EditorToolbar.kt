@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic.codeeditor

import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.externals.reactace.AceModes
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.frontend.utils.selectorBuilder
import com.saveourtool.save.frontend.utils.withUnusedArg
import csstype.ClassName
import react.ChildrenBuilder
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div

private val toolbarCard = cardComponent(isBordered = true)

/**
 * @param selectedMode
 * @param selectedTheme
 * @param selectedFileType
 * @param hasUncommittedChanges
 * @param setSelectedMode
 * @param setSelectedTheme
 * @param onUploadChanges
 * @param onReloadChanges
 * @param onResultReload
 * @param onRunExecution
 * @param onFileTypeChange
 */
@Suppress(
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
    "TOO_LONG_FUNCTION",
    "LongMethod",
)
fun ChildrenBuilder.displayCodeEditorToolbar(
    selectedMode: AceModes,
    selectedTheme: AceThemes,
    selectedFileType: FileType?,
    hasUncommittedChanges: Map<FileType, Boolean>,
    setSelectedMode: (String) -> Unit,
    setSelectedTheme: (String) -> Unit,
    onUploadChanges: () -> Unit,
    onReloadChanges: () -> Unit,
    onResultReload: () -> Unit,
    onRunExecution: () -> Unit,
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
                    val buttonStyle = if (hasUncommittedChanges.getValue(fileType)) {
                        "warning"
                    } else {
                        "primary"
                    }
                    buttonBuilder(
                        fileType.prettyName,
                        buttonStyle,
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
