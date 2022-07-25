/**
 * Component for Contest Registration
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.frontend.externals.modal.modal
import com.saveourtool.save.frontend.utils.*

import csstype.ClassName
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select

import kotlinx.coroutines.await

/**
 * A [FC] for a card that allows user to:
 *  1. Choose a projects that should be enrolled for a contest
 *  2. Choose a contest that the project should be enrolled for
 */
val contestEnrollerComponent = contestEnrollerComponent()

/**
 * Abstract class needed for more convenient parameter pass
 */
sealed class NameProps

/**
 * Data class for project path pass
 *
 * @property organizationName name of an organization in which project with [projectName] is in
 * @property projectName name of a project
 */
data class ProjectNameProps(
    val organizationName: String,
    val projectName: String,
) : NameProps()

/**
 * Data class for contest path pass
 *
 * @property contestName name of a contest
 */
data class ContestNameProps(
    val contestName: String,
) : NameProps()

/**
 * [Props] for [contestEnrollerComponent]
 */
external interface ContestEnrollerProps : Props {
    /**
     * Either a name of a contest that is preselected for participating or a name of a project that is going to be enrolled for a contest
     */
    var nameProps: NameProps

    /**
     * Lambda invoked when got response for enrollment request
     */
    var onResponseChanged: (String) -> Unit
}

/**
 * Modal that shows [contestEnrollerComponent]
 *
 * @param isModalOpen flag that indicates whether the modal should be shown or not
 * @param selectedNameProps name of a current contest or a current project
 * @param onResponse callback invoked when enrollment response is received
 * @param onCloseButtonPressed callback invoked when close button was clicked
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
fun ChildrenBuilder.showContestEnrollerModal(
    isModalOpen: Boolean,
    selectedNameProps: NameProps,
    onCloseButtonPressed: () -> Unit,
    onResponse: (String) -> Unit,
) {
    modal { props ->
        props.isOpen = isModalOpen
        contestEnrollerComponent {
            nameProps = selectedNameProps
            onResponseChanged = onResponse
        }
        div {
            className = ClassName("d-sm-flex align-items-center justify-content-center mt-4")
            button {
                className = ClassName("btn btn-primary")
                type = ButtonType.button
                onClick = {
                    onCloseButtonPressed()
                }
                +"Cancel"
            }
        }
    }
}

/**
 * A [FC] for a card that allows user to:
 *  1. Choose a projects that should be enrolled for a contest
 *  2. Choose a contest that the project should be enrolled for
 *
 * @return a functional component representing a contest enroller
 */
@Suppress(
    "LongMethod",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
    "TOO_LONG_FUNCTION",
    "MAGIC_NUMBER",
    "ComplexMethod",
)
private fun contestEnrollerComponent() = FC<ContestEnrollerProps> { props ->
    val (isContestSelector, _) = useState(props.nameProps is ProjectNameProps)
    val (organizationName, setOrganizationName) = useState(if (isContestSelector) {
        (props.nameProps as ProjectNameProps).organizationName
    } else {
        null
    })
    val (projectName, setProjectName) = useState(if (isContestSelector) {
        (props.nameProps as ProjectNameProps).projectName
    } else {
        null
    })
    val (contestName, setContestName) = useState(if (!isContestSelector) {
        (props.nameProps as ContestNameProps).contestName
    } else {
        null
    })

    val (availableOptions, setAvailableOptions) = useState(emptyList<String>())
    useRequest(isDeferred = false) {
        val availableVariants = get(
            if (isContestSelector) {
                "$apiUrl/contests/$organizationName/$projectName/eligible-contests"
            } else {
                "$apiUrl/contests/$contestName/eligible-projects"
            },
            headers = Headers().also {
                it.set("Accept", "application/json")
                it.set("Content-Type", "application/json")
            },
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<List<String>>()
            }
        setAvailableOptions(availableVariants)
    }()

    val enrollRequest = useRequest {
        val responseFromBackend = get(
            "$apiUrl/contests/$contestName/enroll?organizationName=$organizationName&projectName=$projectName",
            headers = Headers(),
            loadingHandler = ::noopLoadingHandler,
        )
            .text()
            .await()
        props.onResponseChanged(responseFromBackend)
    }

    div {
        className = ClassName("mt-0 pt-2 pr-0 pl-0 pb-2")
        div {
            className = ClassName("mb-2")
            select {
                className = ClassName("custom-select form-control")
                option {
                    disabled = true
                    selected = true
                    value = null
                    +if (isContestSelector) {
                        "Choose a contest..."
                    } else {
                        "Choose a project..."
                    }
                }
                availableOptions.forEach {
                    option {
                        +it
                    }
                    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
                    onChange = {
                        val selectedOption = it.target.value
                        if (selectedOption.isNotBlank()) {
                            if (isContestSelector) {
                                setContestName(selectedOption)
                            } else {
                                val (newOrganizationName, newProjectName) = selectedOption.split("/")
                                setOrganizationName(newOrganizationName)
                                setProjectName(newProjectName)
                            }
                        }
                    }
                }
            }
        }
        div {
            className = ClassName("d-flex justify-content-center")
            button {
                className = ClassName("btn btn-primary d-flex justify-content-center")
                +"Participate"
                onClick = {
                    enrollRequest()
                }
                disabled = projectName.isNullOrBlank() || organizationName.isNullOrBlank() || contestName.isNullOrBlank()
            }
        }
    }
}
