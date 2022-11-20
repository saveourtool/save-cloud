/**
 * Function component for build window
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.demo.diktat.DemoAdditionalParams
import com.saveourtool.save.demo.diktat.DemoRunRequest
import com.saveourtool.save.frontend.components.basic.codeeditor.codeEditorComponent
import com.saveourtool.save.utils.Languages
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.utils.*
import csstype.ClassName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.useState

/**
 * DemoComponent [Props]
 */
@Suppress("TYPE_ALIAS")
external interface BuilderComponentProps : Props {
    /**
     * modal for builder window
     */
    var builderModal: (ChildrenBuilder) -> Unit
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "KDOC_WITHOUT_RETURN_TAG",
    "LongMethod",
    "TYPE_ALIAS"
)
val cpgDemoComponent = FC<BuilderComponentProps> { props ->
    val (selectedLanguage, setSelectedLanguage) = useState(Languages.KOTLIN)
    val (codeLines, setCodeLines) = useState("")
    val (selectedTheme, setSelectedTheme) = useState(AceThemes.preferredTheme)
    val sendRunRequest = useDeferredRequest {
        val data = Json.encodeToString(DemoRunRequest(
            codeLines.split("\n"),
            DemoAdditionalParams(language = selectedLanguage)
        ))
            post(
                "$cpgDemoApiUrl/upload-code",
                headers = jsonHeaders,
                body = data,
                loadingHandler = ::loadingHandler,
                responseHandler = ::noopResponseHandler
            )
        }

    div {
        className = ClassName("")
        div {
            className = ClassName("row")
            div {
                className = ClassName("col-4")
                codeEditorComponent {
                    editorTitle = "Input code"
                    this.selectedTheme = selectedTheme
                    selectedMode = selectedLanguage
                    savedText = codeLines
                    draftText = codeLines
                    onDraftTextUpdate = { code ->
                        setCodeLines(code)
                    }
                    isDisabled = false
                }

                div {
                    className = ClassName("card-body row d-flex justify-content-center")
                    div {
                        className = ClassName("mr-1")
                        selectorBuilder(
                            selectedTheme.themeName,
                            AceThemes.values().map { it.themeName },
                            "custom-select",
                        ) { event ->
                            setSelectedTheme {
                                AceThemes.values().find { it.themeName == event.target.value }!!
                            }
                        }
                    }
                    div {
                        className = ClassName("mr-1")
                        selectorBuilder(
                            selectedLanguage.prettyName,
                            Languages.values().map { it.prettyName },
                            "custom-select"
                        ) { event ->
                            setSelectedLanguage {
                                Languages.values().find { it.prettyName == event.target.value }!!
                            }
                        }
                    }
                    div {
                        buttonBuilder("Send run request") {
                            sendRunRequest()
                        }
                    }
                }
            }

            div {
                className = ClassName("col-8")
                h6 {
                    className = ClassName("text-center text-primary")
                    +"Graph"
                }
                div {
                    className = ClassName("card card-body")
                }
                props.builderModal(this)
            }
        }
    }
}
