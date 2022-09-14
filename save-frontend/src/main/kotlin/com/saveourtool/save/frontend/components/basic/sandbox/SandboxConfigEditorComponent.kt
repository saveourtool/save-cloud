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
 * Config editor functional component
 */
val sandboxConfigEditorComponent = sandboxConfigEditorComponent()

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
private val modeSelector = selectFormRequired<AceModes>()

/**
 * SandboxConfigEditor functional component [Props]
 */
external interface SandboxConfigEditorComponentProps : Props {
    /**
     * Currently inputted text
     */
    var configText: String

    /**
     * Callback invoked on editor change
     */
    var onConfigTextUpdate: (String) -> Unit

    /**
     * Currently selected AceEditor theme
     */
    var selectedTheme: AceThemes
}

/**
 * @return ReactElement
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun sandboxConfigEditorComponent() = FC<SandboxConfigEditorComponentProps> { props ->
    val (selectedMode, _) = useState(AceModes.TOML)
    div {
        h6 {
            className = ClassName("text-center text-primary")
            +"Config editor"
        }
        div {
            className = ClassName("d-flex justify-content-center mb-3")
            modeSelector {
                formType = InputTypes.ACE_MODE_SELECTOR
                validInput = null
                getData = {
                    listOf(selectedMode)
                }
                classes = "col-6"
                selectClasses = "custom-select custom-select-sm"
                disabled = true
                selectedValue = selectedMode.modeName
                dataToString = { it.modeName }
                errorMessage = null
                notFoundErrorMessage = null
            }
        }
        aceBuilder(props.configText, AceModes.TOML, props.selectedTheme, props.onConfigTextUpdate)
    }
}
