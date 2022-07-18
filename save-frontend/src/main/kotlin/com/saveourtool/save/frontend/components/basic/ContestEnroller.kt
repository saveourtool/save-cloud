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
 * [Props] for [contestEnrollerComponent]
 */
external interface ContestEnrollerProps : Props {
    /**
     * Name of the organization that the project belongs to
     *
     * Either organizationName and projectName or contestName should be set
     */
    var organizationName: String?

    /**
     * Name of the project that is planned to be enrolled for a contest
     *
     * Either organizationName and projectName or contestName should be set
     */
    var projectName: String?

    /**
     * Name of a contest that the chosen project should be enrolled for
     *
     * Either organizationName and projectName or contestName should be set
     */
    var contestName: String?

    /**
     * Lambda invoked when got response for enrollment request
     */
    var onResponseChanged: (String) -> Unit
}

/**
 * Modal that shows [contestEnrollerComponent]
 *
 * @param isModalOpen flag that indicates whether the modal should be shown or not
 * @param selectedContestName name of a current contest (should be null if [selectedProjectName] or [selectedOrganizationName] is not null)
 * @param selectedOrganizationName name of an organization that project is from (should be null if [selectedContestName] is not null)
 * @param selectedProjectName name of a current project (should be null if [selectedContestName] is not null)
 * @param onResponse callback invoked when enrollment response is received
 * @param onCloseButtonPressed callback invoked when close button was clicked
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
fun ChildrenBuilder.showContestEnrollerModal(
    isModalOpen: Boolean,
    selectedContestName: String?,
    selectedOrganizationName: String?,
    selectedProjectName: String?,
    onResponse: (String) -> Unit,
    onCloseButtonPressed: () -> Unit,
) {
    modal { props ->
        props.isOpen = isModalOpen
        contestEnrollerComponent {
            contestName = selectedContestName
            projectName = selectedProjectName
            organizationName = selectedOrganizationName
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
    val (organizationName, setOrganizationName) = useState(props.organizationName)
    val (projectName, setProjectName) = useState(props.projectName)
    val (contestName, setContestName) = useState(props.contestName)
    val (isContestSelector, _) = useState(props.contestName == null)
    require(isContestSelector && props.organizationName != null && props.projectName != null || !isContestSelector && props.contestName != null)
    val (availableOptions, setAvailableOptions) = useState(emptyList<String>())
    useRequest(isDeferred = false) {
        val availableVariants = get(
            if (isContestSelector) { "$apiUrl/contests/$organizationName/$projectName/eligible-contests" } else { "$apiUrl/contests/$contestName/eligible-projects" },
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
                    +(props.contestName?.let { "Choose a project..." } ?: "Choose a contest...")
                }
                availableOptions.forEach {
                    option {
                        +it
                    }
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
            }
        }
    }
}
