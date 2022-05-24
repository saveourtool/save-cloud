/**
 * Function component for project info and edit support
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.externals.fontawesome.faCheck
import com.saveourtool.save.frontend.externals.fontawesome.faTimesCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon

import csstype.ClassName
import react.FC
import react.Props
import react.StateSetter
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.useEffect
import react.useRef
import react.useState

private val projectInformationHeaders = mapOf(
    "name" to "Tested tool name: ",
    "description" to "Description: ",
    "url" to "Tested tool Url: ",
)

/**
 * ProjectInfo component props
 */
external interface ProjectInfoProps : Props {
    /**
     * Project passed from parent component that should be used for initial values
     */
    var project: Project?

    /**
     * Whether fields for project info should be disabled
     */
    var isEditDisabled: Boolean?
}

/**
 * @param turnEditMode
 * @param onProjectSave
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "KDOC_WITHOUT_RETURN_TAG",
    "LongMethod",
    "TYPE_ALIAS"
)
fun projectInfo(
    turnEditMode: (isOff: Boolean) -> Unit,
    onProjectSave: (draftProject: Project?, setDraftProject: StateSetter<Project?>) -> Unit,
) = FC<ProjectInfoProps> { props ->
    val projectRef = useRef(props.project)
    val (draftProject, setDraftProject) = useState(props.project)
    useEffect(arrayOf<dynamic>(props.project)) {
        if (projectRef.current !== props.project) {
            setDraftProject(props.project)
            projectRef.current = props.project
        }
    }
    val idToValue = mapOf(
        "name" to draftProject?.name,
        "url" to draftProject?.url,
        "description" to draftProject?.description,
    )
    val idToValueSetter: Map<String, (String) -> Project?> = mapOf(
        "name" to { draftProject?.copy(name = it) },
        "url" to { draftProject?.copy(url = it) },
        "description" to { draftProject?.copy(description = it) },
    )
    form {
        div {
            className = ClassName("row g-3 ml-3 mr-3 pb-2 pt-2  border-bottom")
            idToValue.forEach { (fieldId, text) ->
                div {
                    className = ClassName("col-md-6 pl-0 pr-0")
                    label {
                        className = ClassName("control-label col-auto justify-content-between pl-0")
                        +projectInformationHeaders[fieldId]!!
                    }
                }
                div {
                    className = ClassName("col-md-6 pl-0")
                    div {
                        className = ClassName("controls col-auto pl-0")
                        input {
                            className = ClassName("form-control-plaintext pt-0 pb-0")
                            type = InputType.text
                            this.id = fieldId
                            value = text ?: ""
                            disabled = if (fieldId == "name") {
                                // temporary workaround for https://github.com/analysis-dev/save-cloud/issues/589#issuecomment-1049674021
                                true
                            } else {
                                props.isEditDisabled ?: true
                            }
                            onChange = { event ->
                                val tg = event.target
                                setDraftProject(idToValueSetter[fieldId]!!(tg.value))
                            }
                        }
                    }
                }
            }
        }

        div {
            className = ClassName("ml-3 mt-2 align-items-right float-right")
            button {
                className = ClassName("btn")
                fontAwesomeIcon(icon = faCheck)
                id = "Save new project info"
                hidden = true
                onClick = {
                    onProjectSave(draftProject, setDraftProject)
                    turnEditMode(true)
                }
            }

            button {
                className = ClassName("btn")
                fontAwesomeIcon(icon = faTimesCircle)
                id = "Cancel"
                hidden = true
                onClick = {
                    setDraftProject(props.project)
                    turnEditMode(true)
                }
            }
        }
    }
}
