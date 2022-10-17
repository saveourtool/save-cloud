/**
 * This file contains function to create ManageGitCredentialsCard
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.organizations.testsuitespermissions

import com.saveourtool.save.frontend.components.basic.testsuiteselector.TestSuiteSelectorPurpose
import com.saveourtool.save.frontend.components.basic.testsuiteselector.testSuiteSelector
import com.saveourtool.save.frontend.components.inputform.inputWithDebounceForString
import com.saveourtool.save.frontend.components.modal.largeTransparentModalStyle
import com.saveourtool.save.frontend.components.modal.modal
import com.saveourtool.save.frontend.components.modal.modalBuilder
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.permission.Rights
import com.saveourtool.save.permission.SetRightsRequest
import com.saveourtool.save.testsuite.TestSuiteDto

import csstype.ClassName
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option

import kotlinx.coroutines.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Fully independent component that allows to manage test suites permissions.
 */
val manageTestSuitePermissionsComponent = manageTestSuitePermissionsComponent()

/**
 * Enum class that defines current state of [manageTestSuitePermissionsComponent] (mostly state of the modal inside component)
 */
enum class PermissionManagerMode {
    /**
     * State when a modal with three input forms is shown: what, where and how to add.
     */
    MAIN,

    /**
     * State when success (or error) message is shown.
     */
    MESSAGE,

    /**
     * Make test suites public or private
     *
     * todo: Not implemented yet
     */
    PUBLISH,

    /**
     * Select test suites that should be managed.
     */
    TEST_SUITES_SELECTOR,
    ;
}

/**
 * Props for ManageGitCredentialsCard
 */
external interface ManageTestSuitePermissionsComponentProps : Props {
    /**
     * name of organization, assumption that it's checked by previous views and valid here
     */
    var organizationName: String

    /**
     * Flag that defines if component is shown or not
     */
    var isModalOpen: Boolean

    /**
     * Callback to hide component
     */
    var closeModal: () -> Unit
}

@Suppress("TOO_MANY_PARAMETERS", "LongParameterList", "TOO_LONG_FUNCTION")
private fun ChildrenBuilder.displayMainWindow(
    selectedTestSuites: List<TestSuiteDto>,
    organizationName: String,
    requestedRights: Rights,
    openTestSuiteSelector: () -> Unit,
    setOrganizationName: (String) -> Unit,
    setRequestedRights: (Rights) -> Unit,
) {
    div {
        div {
            className = ClassName("row mb-2")
            label {
                className = ClassName("col-1 float-left align-self-center m-0")
                +"1. "
            }
            div {
                className = ClassName("pl-0 col-11")
                input {
                    className = ClassName("form-control")
                    placeholder = "Choose test suites to share..."
                    value = selectedTestSuites.map { it.name }.sorted().joinToString(", ")
                    onClick = {
                        openTestSuiteSelector()
                    }
                }
            }
        }
        div {
            className = ClassName("row mb-2")
            label {
                className = ClassName("col-1 float-left align-self-center m-0")
                +"2. "
            }
            div {
                className = ClassName("pl-0 col-11")
                inputWithDebounceForString {
                    selectedOption = organizationName
                    setSelectedOption = { setOrganizationName(it) }
                    getOptionFromString = { it }
                    getString = { it }
                    getUrlForOptions = { prefix -> "$apiUrl/organizations/get/by-prefix?prefix=$prefix" }
                    placeholder = "Select organization name..."
                    decodeListFromJsonString = { it.decodeFromJsonString() }
                    getHTMLDataListElementFromOption = { childrenBuilder, organizationName ->
                        with(childrenBuilder) {
                            option { value = organizationName }
                        }
                    }
                }
            }
        }
        div {
            className = ClassName("row")
            label {
                className = ClassName("col-1 float-left align-self-center m-0")
                +"3. "
            }
            div {
                className = ClassName("pl-0 col-11")
                selectorBuilder(requestedRights.toString(), Rights.values().map { it.toString() }) {
                    setRequestedRights(Rights.valueOf(it.target.value))
                }
            }
        }
    }
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun manageTestSuitePermissionsComponent() = FC<ManageTestSuitePermissionsComponentProps> { props ->
    val (selectedTestSuites, setSelectedTestSuites) = useState<List<TestSuiteDto>>(emptyList())
    val (organizationName, setOrganizationName) = useState("")
    val (requiredRights, setRequiredRights) = useState(Rights.NONE)

    val (currentMode, setCurrentMode) = useState(PermissionManagerMode.MAIN)
    val (backendResponseMessage, setBackendResponseMessage) = useState("")
    val sendTransferRequest = useDeferredRequest {
        val response = post(
            url = "$apiUrl/test-suites/${props.organizationName}/batch-set-rights",
            headers = jsonHeaders,
            body = Json.encodeToString(SetRightsRequest(organizationName, requiredRights, selectedTestSuites.map { it.requiredId() })),
            loadingHandler = ::noopLoadingHandler,
        )
        val message = if (response.ok) {
            response.text().await()
        } else {
            response.unpackMessage()
        }
        setBackendResponseMessage(message)
    }

    val clearFields = {
        setSelectedTestSuites(emptyList())
        setCurrentMode(PermissionManagerMode.MAIN)
        setBackendResponseMessage("")
        setRequiredRights(Rights.NONE)
        setOrganizationName("")
    }

    modal { modalProps ->
        modalProps.isOpen = props.isModalOpen
        modalProps.style = largeTransparentModalStyle
        modalBuilder(
            title = "Test Suite Permission Manager",
            classes = "modal-lg modal-dialog-scrollable",
            onCloseButtonPressed = { props.closeModal() },
            bodyBuilder = {
                when (currentMode) {
                    PermissionManagerMode.MAIN -> displayMainWindow(
                        selectedTestSuites,
                        organizationName,
                        requiredRights,
                        { setCurrentMode(PermissionManagerMode.TEST_SUITES_SELECTOR) },
                        { setOrganizationName(it) }
                    ) {
                        setRequiredRights(it)
                    }
                    PermissionManagerMode.TEST_SUITES_SELECTOR -> testSuiteSelector {
                        this.onTestSuiteUpdate = {
                            setSelectedTestSuites(it)
                        }
                        this.preselectedTestSuites = selectedTestSuites
                        this.selectorPurpose = TestSuiteSelectorPurpose.PRIVATE
                        this.currentOrganizationName = props.organizationName
                    }
                    PermissionManagerMode.PUBLISH -> {}
                    PermissionManagerMode.MESSAGE -> +backendResponseMessage
                }
            }
        ) {
            when (currentMode) {
                PermissionManagerMode.MAIN -> buttonBuilder("Apply", isDisabled = selectedTestSuites.isEmpty()) {
                    sendTransferRequest()
                    setCurrentMode(PermissionManagerMode.MESSAGE)
                }
                PermissionManagerMode.TEST_SUITES_SELECTOR -> buttonBuilder("Apply", isDisabled = selectedTestSuites.isEmpty()) {
                    setCurrentMode(PermissionManagerMode.MAIN)
                }
                PermissionManagerMode.PUBLISH -> buttonBuilder("Apply", isDisabled = selectedTestSuites.isEmpty()) {
                    throw NotImplementedError("Making test suite public and backwards will be featured in Ph. 2")
                }
                else -> {}
            }
            buttonBuilder("Cancel", "secondary") {
                when (currentMode) {
                    PermissionManagerMode.MESSAGE -> {
                        clearFields()
                        props.closeModal()
                    }
                    PermissionManagerMode.MAIN -> {
                        clearFields()
                        props.closeModal()
                    }
                    PermissionManagerMode.TEST_SUITES_SELECTOR -> {
                        setSelectedTestSuites(emptyList())
                        setCurrentMode(PermissionManagerMode.MAIN)
                    }
                    PermissionManagerMode.PUBLISH -> {
                        setCurrentMode(PermissionManagerMode.MAIN)
                        throw NotImplementedError("Making test suite public and backwards will be featured in Ph. 2")
                    }
                }
            }
        }
    }
}
