@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.sandbox

import com.saveourtool.save.frontend.components.basic.selectFormRequired
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.externals.reactace.AceModes
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.externals.reactace.aceBuilder
import csstype.ClassName
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.useState

/**
 * Test component displayed in SandboxView
 */
val sandboxCodeEditorComponent = sandboxCodeEditorComponent()

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
private val modeSelector = selectFormRequired<AceModes>()

/**
 * SandboxTestEditor functional component props
 */
external interface SandboxCodeEditorComponentProps : Props {
    /**
     * Currently inputted code
     */
    var codeText: String

    /**
     * Callback invoked on ace editor change
     */
    var onCodeTextUpdate: (String) -> Unit

    /**
     * Currently selected theme for AceEditor
     */
    var selectedTheme: AceThemes
}

private fun sandboxCodeEditorComponent() = FC<SandboxCodeEditorComponentProps> { props ->
    val (selectedMode, setSelectedMode) = useState(AceModes.KOTLIN)
    div {
        h6 {
            className = ClassName("text-center text-primary")
            +"Code editor"
        }
        div {
            className = ClassName("d-flex justify-content-center mb-3")
            modeSelector {
                formType = InputTypes.ACE_MODE_SELECTOR
                validInput = null
                getData = {
                    AceModes.values().toList()
                }
                classes = "col-6"
                selectedValue = selectedMode.modeName
                dataToString = { it.modeName }
                errorMessage = null
                notFoundErrorMessage = null
                onChangeFun = { mode ->
                    mode?.let {
                        setSelectedMode(mode)
                    }
                }
            }
        }
        aceBuilder(props.codeText, selectedMode, props.selectedTheme, props.onCodeTextUpdate)
    }
}
