@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package org.cqfn.save.frontend.components.basic

import kotlinx.html.ButtonType
import kotlinx.html.InputType
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
import org.w3c.dom.HTMLInputElement
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
//    var users: List<UserInfo>?

    /**
     * Flag that indicates if project is public
     */
    var isPublic: Boolean?

    /**
     * Number of containers available for this project
     */
    var numberOfContainers: Int?

    /**
     * Email that is connected to the project
     */
    var emailFromInput: String?

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

    div("row justify-content-center mb-2") {
        // ===================== LEFT COLUMN =======================================================================
        div("col-3") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Users"
            }

//            div("col-xl col-md-6 mb-4") {
//                +"TEST"
//            }
        }
        // ===================== CENTER COLUMN =======================================================================
        div("col-6 mb-2") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Main settings"
            }
            child(cardComponent(isBordered = false, hasBg = true) {
                div("row d-flex align-items-center mt-2 ml-2 mr-2") {
                    div("text-left col-3") {
                        +"Project email:"
                    }
                    div("input-group-prepend col-9") {
                        input(type = InputType.text) {
                            attrs["class"] = "form-control"
                            attrs {
                                props.emailFromInput?.let {
                                    defaultValue = it
                                } ?: props.project?.email?.let {
                                    defaultValue = it
                                }
                                placeholder = "johndoe@example.com"
                                onChangeFunction = {
                                    props.emailFromInput = (it.target as HTMLInputElement).value
                                }
                            }
                        }
                    }
                }
                div("row d-flex align-items-center mt-2 mr-2 ml-2") {
                    div("text-left col-3") {
                        +"Project visibility:"
                    }
                    div ("row d-flex justify-content-between col-9") {
                        div("form-check form-switch mt-2 col-3") {
                            input(classes = "form-check-input") {
                                attrs["name"] = "projectVisibility"
                                attrs["type"] = "radio"
                                attrs["id"] = "isProjectPublicSwitch"
                            }
                            label("form-check-label") {
                                attrs["htmlFor"] = "isProjectPublicSwitch"
                                +"Public"
                            }
                        }
                        div("form-check form-switch mt-2 col-3") {
                            input(classes = "form-check-input") {
                                attrs["name"] = "projectVisibility"
                                attrs["type"] = "radio"
                                attrs["id"] = "isProjectPrivateSwitch"
                            }
                            label("form-check-label") {
                                attrs["htmlFor"] = "isProjectPrivateSwitch"
                                +"Private"
                            }
                        }
                    }
                }
                div("row d-flex align-items-center mt-2 mr-2 ml-2") {
                    div("text-left col-3") {
                        +"Number of containers:"
                    }
                    div ("row d-flex justify-content-between col-9") {
                        div("form-check form-switch mt-2 col-3") {
                            input(classes = "form-check-input") {
                                attrs["name"] = "projectVisibility"
                                attrs["type"] = "radio"
                                attrs["id"] = "isProjectPublicSwitch"
                            }
                            label("form-check-label") {
                                attrs["htmlFor"] = "isProjectPublicSwitch"
                                +"Public"
                            }
                        }
                        div("form-check form-switch mt-2 col-3") {
                            input(classes = "form-check-input") {
                                attrs["name"] = "projectVisibility"
                                attrs["type"] = "radio"
                                attrs["id"] = "isProjectPrivateSwitch"
                            }
                            label("form-check-label") {
                                attrs["htmlFor"] = "isProjectPrivateSwitch"
                                +"Private"
                            }
                        }
                    }
                }

            })
        }


        // ===================== RIGHT COLUMN =======================================================================
        div("col-2") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Vital"
            }
            div("row d-flex justify-content-center") {
                div("ml-3 d-sm-flex align-items-center justify-content-center mt-2") {
                    button(type = ButtonType.button, classes = "btn btn-sm btn-primary") {
                        attrs.onClickFunction = {
                            updateProjectSettings(props.project)
                        }
                        +"Save changes"
                    }
                }
                div("ml-3 d-sm-flex align-items-center justify-content-center mt-2") {
                    button(type = ButtonType.button, classes = "btn btn-sm btn-danger") {
                        attrs.onClickFunction = {
                            deleteProjectCallback()
                        }
                        +"Delete project"
                    }
                }
            }
        }
    }
}
