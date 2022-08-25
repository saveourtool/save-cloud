@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.SourceSaveStatus
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.inputform.inputTextDisabled
import com.saveourtool.save.frontend.components.inputform.inputTextFormOptionalWrapperConst
import com.saveourtool.save.frontend.components.inputform.inputTextFormRequired
import com.saveourtool.save.frontend.externals.fontawesome.faTimesCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.externals.modal.CssProperties
import com.saveourtool.save.frontend.externals.modal.Styles
import com.saveourtool.save.frontend.externals.modal.modal
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.v1

import csstype.ClassName
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.useState

import kotlin.js.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val testSuiteSourceCreationComponent = testSuiteSourceCreationComponent()

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
private val gitSelectionForm = selectFormRequired<GitDto>()

/**
 * [Props] for [testSuiteSourceCreationComponent]
 */
external interface TestSuiteSourceCreationProps : Props {
    /**
     * Name of a current organization
     */
    var organizationName: String

    /**
     * Callback invoked on successful save
     */
    var onSuccess: () -> Unit
}

/**
 * @param isOpen
 * @param organizationName
 * @param onSuccess
 * @param onClose
 */
@Suppress("TOO_LONG_FUNCTION")
fun ChildrenBuilder.showTestSuiteSourceCreationModal(
    isOpen: Boolean,
    organizationName: String,
    onSuccess: () -> Unit,
    onClose: () -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = Styles(
            content = json(
                "top" to "10%",
                "left" to "30%",
                "right" to "30%",
                "bottom" to "auto",
                "position" to "absolute",
                "overflow" to "hide",
            ).unsafeCast<CssProperties>()
        )
        div {
            className = ClassName("d-flex justify-content-between")
            h5 {
                className = ClassName("modal-title mb-3")
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
            testSuiteSourceCreationComponent {
                this.organizationName = organizationName
                this.onSuccess = onSuccess
            }
        }
    }
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun testSuiteSourceCreationComponent() = FC<TestSuiteSourceCreationProps> { props ->
    val (testSuiteSource, setTestSuiteSource) = useState(TestSuitesSourceDto.empty.copy(organizationName = props.organizationName))
    val (saveStatus, setSaveStatus) = useState<SourceSaveStatus?>(null)
    val fetchTestSuiteSource = useDeferredRequest {
        post(
            url = "$apiUrl/test-suites-sources/${testSuiteSource.organizationName}/${testSuiteSource.name}/fetch",
            headers = jsonHeaders,
            body = undefined,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        )
    }
    val onSubmitButtonPressed = useDeferredRequest {
        val response = post(
            url = "/api/$v1/test-suites-sources/create",
            headers = jsonHeaders,
            body = Json.encodeToString(testSuiteSource),
            loadingHandler = ::loadingHandler,
            responseHandler = ::responseHandlerWithValidation,
        )
        if (response.ok) {
            fetchTestSuiteSource()
            props.onSuccess()
        } else if (response.isConflict()) {
            setSaveStatus(response.decodeFromJsonString<SourceSaveStatus>())
        }
    }

    div {
        inputTextFormRequired(
            InputTypes.SOURCE_NAME,
            testSuiteSource.name,
            testSuiteSource.validateName() && saveStatus != SourceSaveStatus.EXIST,
            "mb-2",
            "Source name",
        ) {
            setTestSuiteSource(testSuiteSource.copy(name = it.target.value))
            if (saveStatus == SourceSaveStatus.EXIST) {
                setSaveStatus(null)
            }
        }
        inputTextDisabled(
            InputTypes.ORGANIZATION_NAME,
            "mb-2",
            "Organization name",
            testSuiteSource.organizationName
        )
        inputTextFormOptionalWrapperConst {
            form = InputTypes.GIT_BRANCH
            textValue = testSuiteSource.branch
            classes = "mb-2"
            name = "Branch"
            validInput = saveStatus != SourceSaveStatus.CONFLICT
            onChangeFun = {
                setTestSuiteSource(testSuiteSource.copy(branch = it.target.value))
                if (saveStatus == SourceSaveStatus.CONFLICT) {
                    setSaveStatus(null)
                }
            }
        }
        inputTextFormOptionalWrapperConst {
            form = InputTypes.SOURCE_TEST_ROOT_PATH
            textValue = testSuiteSource.testRootPath
            classes = "mb-2"
            name = "Test root path"
            validInput = testSuiteSource.validateTestRootPath() && saveStatus != SourceSaveStatus.CONFLICT
            onChangeFun = {
                setTestSuiteSource(testSuiteSource.copy(testRootPath = it.target.value))
                if (saveStatus == SourceSaveStatus.CONFLICT) {
                    setSaveStatus(null)
                }
            }
        }
        gitSelectionForm {
            formType = InputTypes.SOURCE_GIT
            validInput = saveStatus != SourceSaveStatus.CONFLICT
            classes = "mb-2"
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
                    if (saveStatus == SourceSaveStatus.CONFLICT) {
                        setSaveStatus(null)
                    }
                }
            }
        }
        inputTextFormOptionalWrapperConst {
            form = InputTypes.DESCRIPTION
            textValue = testSuiteSource.description
            classes = "mb-2"
            name = "Description"
            validInput = true
            onChangeFun = {
                setTestSuiteSource(testSuiteSource.copy(description = it.target.value))
            }
        }
        div {
            className = ClassName("d-flex justify-content-center")
            button {
                className = ClassName("btn btn-primary mt-2 mb-2")
                disabled = !testSuiteSource.validate() || saveStatus != null
                onClick = {
                    onSubmitButtonPressed()
                }
                +"Submit"
            }
        }
        saveStatus?.let {
            div {
                className = ClassName("invalid-feedback d-block text-center")
                +it.message
            }
        }
    }
}
