package org.cqfn.save.frontend.components.basic

import kotlinx.html.InputType
import kotlinx.html.hidden
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.externals.fontawesome.faCheck
import org.cqfn.save.frontend.externals.fontawesome.faTimesCircle
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.Props
import react.dom.attrs
import react.dom.button
import react.dom.div
import react.dom.form
import react.dom.input
import react.dom.label
import react.fc
import react.useEffect
import react.useRef
import react.useState

external interface ProjectInfoProps : Props {
    var project: Project?
    var isEditDisabled: Boolean?
}

fun projectInfo(
    turnEditMode: (isOff: Boolean) -> Unit,
    onProjectSave: (draftProject: Project?, event: Event) -> Unit,
) = fc<ProjectInfoProps> { props ->
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
        div("row g-3 ml-3 mr-3 pb-2 pt-2  border-bottom") {
            idToValue.forEach { (id, text) ->
                    div("col-md-6 pl-0 pr-0") {
                        label(classes = "control-label col-auto justify-content-between pl-0") {
                            +projectInformationHeaders[id]!!
                        }
                    }
                    div("col-md-6 pl-0") {
                        div("controls col-auto pl-0") {
                            input(InputType.text, classes = "form-control-plaintext pt-0 pb-0") {
                                attrs {
                                    this.id = id
                                    value = text ?: ""
                                    disabled = if (id == "name") {
                                        // temporary workaround for https://github.com/analysis-dev/save-cloud/issues/589#issuecomment-1049674021
                                        true
                                    } else {
                                        props.isEditDisabled ?: true
                                    }
                                    onChangeFunction = { event ->
                                        val tg = event.target as HTMLInputElement
                                        setDraftProject(idToValueSetter[id]!!(tg.value))
                                    }
                                }
                            }
                        }
                    }
                }
        }

        div("ml-3 mt-2 align-items-right float-right") {
            button(classes = "btn") {
                fontAwesomeIcon {
                    attrs.icon = faCheck
                }
                attrs.id = "Save new project info"
                attrs.hidden = true
                attrs.onClickFunction = {
                    onProjectSave(draftProject, it)
                    turnEditMode(true)
                }
            }

            button(classes = "btn") {
                fontAwesomeIcon {
                    attrs.icon = faTimesCircle
                }
                attrs.id = "Cancel"
                attrs.hidden = true
                attrs.onClickFunction = {
                    setDraftProject(props.project)
                    turnEditMode(true)
                }
            }
        }
    }
}

private val projectInformationHeaders = mutableMapOf(
    "name" to "Tested tool name: ",
    "description" to "Description: ",
    "url" to "Tested tool Url: ",
)
