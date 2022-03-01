package org.cqfn.save.frontend.components.basic

import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.externals.fontawesome.faCheck
import org.cqfn.save.frontend.externals.fontawesome.faTimesCircle

import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.input
import react.dom.html.InputType
import react.dom.html.ReactHTML.label
import react.FC
import react.useEffect
import react.useRef
import react.useState

import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon

private val projectInformationHeaders = mapOf(
    "name" to "Tested tool name: ",
    "description" to "Description: ",
    "url" to "Tested tool Url: ",
)

external interface ProjectInfoProps : Props {
    var project: Project?
    var isEditDisabled: Boolean?
}

/**
 * @param turnEditMode
 * @param onProjectSave
 * @return
 */
fun projectInfo(
    turnEditMode: (isOff: Boolean) -> Unit,
    onProjectSave: (draftProject: Project?) -> Unit,
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
    val idToValueSetter = mapOf(
        "name" to { s: String -> draftProject?.copy(name = s) },
        "url" to { s: String -> draftProject?.copy(url = s) },
        "description" to { s: String -> draftProject?.copy(description = s) },
    )
    form {
        div {
            className = "row g-3 ml-3 mr-3 pb-2 pt-2  border-bottom"
            idToValue.forEach { (fieldId, text) ->
                div {
                    className = "col-md-6 pl-0 pr-0"
                    label {
                        className = "control-label col-auto justify-content-between pl-0"
                        +projectInformationHeaders[fieldId]!!
                    }
                }
                div {
                    className = "col-md-6 pl-0"
                    div {
                        className = "controls col-auto pl-0"
                        input {
                            className = "form-control-plaintext pt-0 pb-0"
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
            className = "ml-3 mt-2 align-items-right float-right"
            button {
                className = "btn"
                fontAwesomeIcon(icon = faCheck)
                id = "Save new project info"
                hidden = true
                onClick = {
                    onProjectSave(draftProject)
                    turnEditMode(true)
                }
            }

            button {
                className = "btn"
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
