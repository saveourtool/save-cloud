package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.frontend.externals.fontawesome.faTimesCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.externals.modal.modal
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.v1
import csstype.ClassName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.fetch.Response
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.useState
import kotlin.js.json

external interface TestSuiteSourceCreationProps: Props {
    var organizationName: String
    var onSuccess: () -> Unit
    var onFailure: (Response) -> Unit
}

val testSuiteSourceCreationComponent = testSuiteSourceCreationComponent()

fun ChildrenBuilder.showTestSuiteSourceCreationModal(
    isOpen: Boolean,
    organizationName: String,
    onSuccess: () -> Unit,
    onFailure: (Response) -> Unit,
    onClose: () -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        div {
            className = ClassName("modal-dialog")
            div {
                className = ClassName("modal-content")
                div {
                    className = ClassName("modal-header")
                    ReactHTML.h5 {
                        className = ClassName("modal-title mb-0")
                        +"Create test suite source"
                    }
                    button {
                        type = ButtonType.button
                        className = ClassName("close")
                        asDynamic()["data-dismiss"] = "modal"
                        ariaLabel = "Close"
                        fontAwesomeIcon(icon = faTimesCircle)
                        onClick = {
                            onClose()
                        }
                    }
                }
                div {
                    className = ClassName("modal-body")
                    testSuiteSourceCreationComponent {
                        this.organizationName = organizationName
                        this.onSuccess = onSuccess
                        this.onFailure = onFailure
                    }
                }
            }
        }

    }
}

private val selectFormRequired = selectFormRequired<GitDto>()

private fun testSuiteSourceCreationComponent() = FC<TestSuiteSourceCreationProps> { props ->
    val (testSuiteSource, setTestSuiteSource) = useState(TestSuitesSourceDto.empty.copy(organizationName = props.organizationName))
    val (conflictErrorMessage, setConflictErrorMessage) = useState<String?>(null)
    val onSubmitButtonPressed = useRequest(dependencies = arrayOf(testSuiteSource)) {
        val response = post(
            url = "/api/$v1/test-suites-sources/${props.organizationName}/get-or-create",
            headers = jsonHeaders,
            body = Json.encodeToString(testSuiteSource),
            loadingHandler = ::loadingHandler,
            responseHandler = ::responseHandlerWithValidation,
        )
        if (response.ok) {
            props.onSuccess()
        } else if (response.isConflict()) {
            setConflictErrorMessage(response.unpackMessage())
        } else {
            props.onFailure(response)
        }
    }

    div {
        inputTextFormRequired(
            InputTypes.SOURCE_NAME,
            testSuiteSource.name,
            testSuiteSource.validateName() || testSuiteSource.name.isEmpty(),
            "",
            "Source name",
        ) {
            setTestSuiteSource(testSuiteSource.copy(name = it.target.value))
        }
        inputTextDisabled(
            InputTypes.ORGANIZATION_NAME,
            "",
            "Organization name",
            testSuiteSource.organizationName
        )
        inputTextFormRequired(
            InputTypes.GIT_BRANCH,
            testSuiteSource.branch,
            true,
            "",
            "Branch",
        ) {
            setTestSuiteSource(testSuiteSource.copy(branch = it.target.value))
        }
        inputTextFormRequired(
            InputTypes.SOURCE_TEST_ROOT_PATH,
            testSuiteSource.testRootPath,
            testSuiteSource.validateTestRootPath(),
            "",
            "Test root path",
        ) {
            setTestSuiteSource(testSuiteSource.copy(testRootPath = it.target.value))
        }
        selectFormRequired {
            formType = InputTypes.SOURCE_GIT
            validInput = null
            classes = ""
            formName = "Git Credentials"
            getData = {
                get(
                    "$apiUrl/organizations/${props.organizationName}/list-git",
                    headers = jsonHeaders,
                    loadingHandler = ::loadingHandler,
                )
                    .unsafeMap {
                        it.decodeFromJsonString()
                    }
            }
            dataToString = { it.url }
            notFoundErrorMessage = "You have no avaliable git credentials in organization ${props.organizationName}"
            selectedValue = testSuiteSource.gitDto.url
            onChangeFun = { git ->
                git?.let {
                    setTestSuiteSource(testSuiteSource.copy(gitDto = it))
                }
            }
        }
        button {
            className = ClassName("btn btn-primary")
            disabled = !testSuiteSource.validate() || conflictErrorMessage != null
            onClick = {
                onSubmitButtonPressed()
            }
            +"Submit"
        }
        conflictErrorMessage?.let {
            div {
                className = ClassName("invalid-feedback d-block text-center")
                +it
            }
        }
    }
}