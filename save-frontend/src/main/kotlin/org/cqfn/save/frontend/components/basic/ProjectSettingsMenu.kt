@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package org.cqfn.save.frontend.components.basic

import kotlinx.browser.document
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.cqfn.save.agent.TestSuiteExecutionStatisticDto
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.ProjectStatus
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.externals.chart.DataPieChart
import org.cqfn.save.frontend.externals.chart.pieChart
import org.cqfn.save.frontend.externals.chart.randomColor
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.unsafeMap
import org.cqfn.save.frontend.utils.useRequest
import org.cqfn.save.permission.SetRoleRequest
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.events.Event

import org.w3c.fetch.Headers
import react.*
import react.dom.*
import react.table.columns

/**
 * ProjectSettingsMenu component props
 */
external interface ProjectSettingsMenuProps : Props {
    /**
     * List of users connected to the project
     */
    var users: List<SetRoleRequest>?

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
 * @param openMenuSettingsFlag
 * @return ReactElement
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod", "MAGIC_NUMBER")
fun projectSettingsMenu(
    deleteProjectCallback: () -> Unit,
    updateProjectSettings: (Project) -> Unit,
    updateNumberOfContainers: (Int) -> Unit,
) = fc<ProjectSettingsMenuProps> { props ->
//    val (latestExecutionStatisticDtos, setLatestExecutionStatisticDtos) = useState(props.latestExecutionStatisticDtos)

//    useRequest(arrayOf(props.executionId, props.latestExecutionStatisticDtos, props.isOpen), isDeferred = false) {
//        if (props.isOpen != true) {
//            val testLatestExecutions = get(
//                url = "$apiUrl/testLatestExecutions?executionId=${props.executionId}&status=${TestResultStatus.PASSED}",
//                headers = Headers().also {
//                    it.set("Accept", "application/json")
//                },
//            )
//                .unsafeMap {
//                    it.decodeFromJsonString<List<TestSuiteExecutionStatisticDto>>()
//                }
//            setLatestExecutionStatisticDtos(testLatestExecutions)
//            openMenuStatisticFlag(true)
//        }
//    }()

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
                        input(type = InputType.text) {
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
                    form ("col-7 form-group row d-flex justify-content-around") {
                        div ("form-check-inline") {
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
                    div ("col-7 row") {
                        div("form-switch") {
                            select("custom-select") {
//                                attrs.value = numberOfContainers
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
                                val project = props.project.copy()
                                if (emailFromInput != props.project.email) {
                                    project.email = emailFromInput
                                }
                                if (isPublic != props.project.public) {
                                    project.public = isPublic
                                }
                                updateProjectSettings(project)
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
