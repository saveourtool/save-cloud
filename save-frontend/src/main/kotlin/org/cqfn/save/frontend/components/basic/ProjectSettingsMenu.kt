@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.entities.Project

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.*
import react.dom.*

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction

/**
 * ProjectSettingsMenu component props
 */
external interface ProjectSettingsMenuProps : Props {
    /**
     * Number of containers available for this project
     */
    var numberOfContainers: Int?

    /**
     * Flag to open Menu
     */
    var isOpen: Boolean?

    /**
     * Current project settings
     */
    var project: Project
}

/**
 * @param deleteProjectCallback
 * @param updateProjectSettings
 * @param updateNumberOfContainers
 * @return ReactElement
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod", "MAGIC_NUMBER")
fun projectSettingsMenu(
    deleteProjectCallback: () -> Unit,
    updateProjectSettings: (Project) -> Unit,
    updateNumberOfContainers: (Int) -> Unit,
) = fc<ProjectSettingsMenuProps> { props ->
    var emailFromInput: String? = props.project.email
    var isPublic: Boolean = props.project.public
    var numberOfContainers: String = props.numberOfContainers?.toString() ?: "1"

    div("row justify-content-center mb-2") {
        // ===================== LEFT COLUMN =======================================================================
        div("col-4") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Users"
            }
            child(cardComponent(isBordered = false, hasBg = true) {

                })
        }
        // ===================== CENTER COLUMN =======================================================================
        div("col-4 mb-2") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Main settings"
            }
            child(cardComponent(isBordered = false, hasBg = true) {
                div("row mt-2 ml-2 mr-2") {
                    div("col-5 text-left align-self-center") {
                        +"Project email:"
                    }
                    div("col-7 input-group-prepend") {
                        input(type = InputType.email) {
                            attrs["class"] = "form-control"
                            attrs {
                                props.project.email?.let {
                                    defaultValue = it
                                }
                                placeholder = "email@example.com"
                                onChangeFunction = {
                                    emailFromInput = (it.target as HTMLInputElement).value
                                    defaultValue = (it.target as HTMLInputElement).value
                                }
                            }
                        }
                    }
                }
                div("row mt-2 ml-2 mr-2") {
                    div("col-5 text-left align-self-center") {
                        +"Project visibility:"
                    }
                    form("col-7 form-group row d-flex justify-content-around") {
                        div("form-check-inline") {
                            input(classes = "form-check-input") {
                                attrs.defaultChecked = isPublic
                                attrs["name"] = "projectVisibility"
                                attrs["type"] = "radio"
                                attrs["id"] = "isProjectPublicSwitch"
                                attrs["value"] = "public"
                            }
                            label("form-check-label") {
                                attrs["htmlFor"] = "isProjectPublicSwitch"
                                +"Public"
                            }
                        }
                        div("form-check-inline") {
                            input(classes = "form-check-input") {
                                attrs.defaultChecked = !isPublic
                                attrs["name"] = "projectVisibility"
                                attrs["type"] = "radio"
                                attrs["id"] = "isProjectPrivateSwitch"
                                attrs["value"] = "private"
                            }
                            label("form-check-label") {
                                attrs["htmlFor"] = "isProjectPrivateSwitch"
                                +"Private"
                            }
                        }
                        attrs.onChangeFunction = {
                            isPublic = (it.target as HTMLInputElement).value == "public"
                        }
                    }
                }
                div("row d-flex align-items-center mt-2 mr-2 ml-2") {
                    div("col-5 text-left") {
                        +"Number of containers:"
                    }
                    div("col-7 row") {
                        div("form-switch") {
                            select("custom-select") {
                                // fixme: later we will need to change amount of containers
                                attrs.disabled = true
                                attrs.onChangeFunction = {
                                    val target = it.target as HTMLSelectElement
                                    numberOfContainers = target.value
                                    attrs.value = numberOfContainers
                                }
                                attrs.id = "numberOfContainers"
                                for (i in 1..8) {
                                    option {
                                        attrs.value = i.toString()
                                        attrs.selected = i.toString() == numberOfContainers
                                        +i.toString()
                                    }
                                }
                            }
                        }
                    }
                }

                hr("") {}
                div("row d-flex justify-content-center") {
                    div("col-3 d-sm-flex align-items-center justify-content-center") {
                        button(type = ButtonType.button, classes = "btn btn-sm btn-primary") {
                            attrs.onClickFunction = {
                                updateProjectSettings(props.project.copy(
                                    email = emailFromInput,
                                    public = isPublic,
                                ))
                                if (numberOfContainers.toInt() != props.numberOfContainers) {
                                    updateNumberOfContainers(numberOfContainers.toInt())
                                }
                            }
                            +"Save changes"
                        }
                    }
                    div("col-3 d-sm-flex align-items-center justify-content-center") {
                        button(type = ButtonType.button, classes = "btn btn-sm btn-danger") {
                            attrs.onClickFunction = {
                                deleteProjectCallback()
                            }
                            +"Delete project"
                        }
                    }
                }
            })
        }
    }
}
