/**
 * A view with project details
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.components.basic.cardComponent
import org.cqfn.save.frontend.externals.modal.modal
import org.cqfn.save.frontend.utils.post
import org.cqfn.save.repository.GitRepository

import org.w3c.dom.HTMLInputElement
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import org.w3c.files.get
import org.w3c.xhr.FormData

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.html.js.*
import react.*
import react.dom.*

/**
 * [RProps] for project view
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectProps : RProps {
    var executionRequest: ExecutionRequest
}

/**
 * [RProps] retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectExecutionRouteProps : RProps {
    var project: Project
    var gitRepository: GitRepository
}

/**
 * [RState] of project view component
 */
external interface ProjectViewState : RState {
    /**
     * Whether error modal from backend is visible
     */
    var isErrorFromBackend: Boolean

    /**
     * Whether error text from backend is visible
     */
    var errorTextFromBackend: String

    var isNoGitUrl: Boolean

    /**
     * Whether error is both types of project were openned
     */
    var isProjectAsGitUrl: Boolean

    var pathToProperty: String?

    var gitUrlProject: String?

    /**
     * Whether error non project types
     */
    var isProjectAsBinaryFile: Boolean

    var isBothProjectOpen: Boolean

    var isNoneProjectOpen: Boolean

    /**
     * Whether error if no binary file
     */
    var isBinaryFileNotSelect: Boolean

    /**
     * Whether error if no property file
     */
    var isPropertyFileNotSelect: Boolean

    /**
     * Whether error if testSuites empty
     */
    var isTestTypesEmpty: Boolean

    var selectedTypes: MutableList<String>

    var editableUrlInput: Boolean

    var binaryFile: File?

    var propertyFile: File?
}

/**
 * A functional RComponent for project view
 *
 * @return a functional component
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
@Suppress("CUSTOM_GETTERS_SETTERS")
class ProjectView : RComponent<ProjectProps, ProjectViewState>() {
    private val testTypesList = listOf("Class", "Comment", "Function", "Variable", "Enum", "Space", "Style", "Another")
    private lateinit var responseFromExecutionRequest: Response

    init {
        // Project as git
        state.isProjectAsGitUrl = false
        state.pathToProperty = null
        state.gitUrlProject = null

        // Project as file
        state.isProjectAsBinaryFile = false
        state.isTestTypesEmpty = false
        state.selectedTypes = mutableListOf()
        state.isBinaryFileNotSelect = false
        state.isPropertyFileNotSelect = false

        // Error cards
        state.isErrorFromBackend = false
        state.isBothProjectOpen = false
        state.isNoneProjectOpen = false
        state.errorTextFromBackend = ""
        state.isNoGitUrl = false

        state.editableUrlInput = true
    }

    private fun submitExecutionRequest() {
        console.log(state.gitUrlProject)
        if (state.isProjectAsBinaryFile && state.isProjectAsGitUrl) {
            setState { isBothProjectOpen = true }
            return
        } else if (!state.isProjectAsBinaryFile && !state.isProjectAsGitUrl) {
            setState { isNoneProjectOpen = true }
            return
        }
        if (state.isProjectAsGitUrl) {
            state.gitUrlProject?.let {

            } ?: run {
                props.executionRequest.gitRepository.url?.let {
                    submitExecutionRequestGit()
                } ?: {
                    setState { isNoGitUrl = true }
                }
            }
        } else {
            if (state.selectedTypes.isEmpty()) {
                setState { isTestTypesEmpty = true }
                return
            }
            state.binaryFile ?: run {
                setState { isBinaryFileNotSelect = true }
                return
            }
            state.propertyFile ?: run {
                setState { isPropertyFileNotSelect = true }
                return
            }
            submitExecutionRequestBinFile()
        }
    }

    private fun submitExecutionRequestBinFile() {
        val headers = Headers()
        val formData = FormData()
        val request = ExecutionRequestForStandardSuites(props.executionRequest.project, state.selectedTypes)
        console.log(JSON.stringify(request))
        formData.append("execution", Blob(arrayOf(JSON.stringify(request)), BlobPropertyBag("application/json")))
        state.propertyFile?.let { formData.append("property", it) }
        state.binaryFile?.let { formData.append("binFile", it) }
        submitRequest("submitExecutionRequestBin", headers, formData)
    }

    private fun submitExecutionRequestGit() {
        val executionRequest = ExecutionRequest(
            project = props.executionRequest.project,
            gitRepository = GitRepository(url = state.gitUrlProject ?: props.executionRequest.gitRepository.url), // init another fields
            propertiesRelativePath = state.pathToProperty ?: "save.properties"
        )
        val jsonExecution = JSON.stringify(executionRequest)
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        submitRequest("submitExecutionRequest", headers, jsonExecution)
    }

    private fun submitRequest(url: String, headers: Headers, body: dynamic) {
        GlobalScope.launch {
            responseFromExecutionRequest = post("http://localhost:5000/$url", headers, body)
        }.invokeOnCompletion {
            if (responseFromExecutionRequest.ok) {
                window.location.href = "${window.location.origin}/history"
            } else {
                setState {
                    isErrorFromBackend = true
                    errorTextFromBackend = responseFromExecutionRequest.statusText
                }
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    override fun RBuilder.render() {
        // modal window for configuring tests run - initially hidden
        runErrorFromBackendModal()
        runErrorNotGitUrl()
        runErrorEmptyTestTypesModal()
        runErrorBothProjectTypesModal()
        runErrorNoneTypeProjectModal()
        runErrorNoneBinaryFile()
        runErrorNonePropertyFile()
        // Page Heading
        div("d-sm-flex align-items-center justify-content-between mb-4") {
            h1("h3 mb-0 text-gray-800") {
                +"Project ${props.executionRequest.project.name}"
            }
        }

        div("row") {
            child(cardComponent {
                div("text-center") {
                    div("card shadow mb-4") {
                        a(classes = "d-block card-header py-3") {
                            attrs {
                                onClickFunction = { setState { isProjectAsGitUrl = !isProjectAsGitUrl } }
                            }
                            h6("m-0 font-weight-bold text-primary") {
                                +"Upload project as git url"
                            }
                        }
                        // Collapse card to load git url
                        div {
                            attrs.classes = if (state.isProjectAsGitUrl) setOf("collapse", "show") else setOf("collapse")
                            div("card-body") {
                                div("pb-3") {
                                    div("d-inline-block") {
                                        h6(classes = "d-inline") {
                                            +"Git url: "
                                        }
                                    }
                                    div("d-inline-block ml-2") {
                                        input(type = InputType.text, name = "itemText2") {
                                            this.key ="itemText2"
                                            attrs {
                                                attrs.autoFocus = state.editableUrlInput
                                                state.gitUrlProject?.let {
                                                    value = it
                                                } ?: run {
                                                    props.executionRequest.gitRepository.url?.let {
                                                        value = it
                                                    }
                                                }
                                                placeholder = "https://github.com/"
                                                onChangeFunction = {
                                                    val target = it.target as HTMLInputElement
                                                    setState {
                                                        gitUrlProject = target.value
                                                        editableUrlInput = true
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                div("pb-3") {
                                    div("d-inline-block") {
                                        h6(classes = "d-inline") {
                                            +"Path to property file: "
                                        }
                                    }
                                    div("d-inline-block ml-2") {
                                        input(type = InputType.text, name = "itemText") {
                                            key = "itemText"
                                            attrs {
                                                attrs.autoFocus = !state.editableUrlInput
                                                state.pathToProperty?.let {
                                                    value = it
                                                } ?: run {
                                                    placeholder = "save.properties"
                                                }
                                                onChangeFunction = {
                                                    val target = it.target as HTMLInputElement
                                                    setState {
                                                        pathToProperty = target.value
                                                        editableUrlInput = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Collapse card to load binary file
                    div("card shadow mb-4") {
                        a(classes = "d-block card-header py-3") {
                            attrs{
                                onClickFunction = {
                                    setState { isProjectAsBinaryFile = !state.isProjectAsBinaryFile}
                                }
                            }
                            h6("m-0 font-weight-bold text-primary") {
                                +"Upload project as binary file"
                            }
                        }
                        div {
                            attrs.classes = if (state.isProjectAsBinaryFile) setOf("collapse", "show") else setOf("collapse")
                            div("card-body") {
                                div("mb-3") {
                                    h6(classes = "d-inline mr-3") {
                                        +"Binary file: "
                                    }
                                    div {
                                        label {
                                            input(type = InputType.file) {
                                                attrs.hidden = true
                                                attrs {
                                                    onChangeFunction = { event ->
                                                        val target = event.target as HTMLInputElement
                                                        setState { binaryFile = target.files?.let { it[0] } }
                                                    }
                                                }
                                            }
                                            strong { + "Choose binary file:" }
                                            +(state.binaryFile?.name ?: "")
                                        }
                                    }
                                }
                                div {
                                    h6(classes = "d-inline mr-3") {
                                        +"Properties : "
                                    }
                                    div {
                                        label {
                                            input(type = InputType.file) {
                                                attrs.hidden = true
                                                attrs {
                                                    onChangeFunction = { event ->
                                                        val target = event.target as HTMLInputElement
                                                        setState { propertyFile = target.files?.let { it[0] } }
                                                    }
                                                }
                                            }
                                            strong { +"Choose property file: " }
                                            +(state.propertyFile?.name ?: "")
                                        }
                                    }
                                }
                            }
                            div {
                                testTypesList.chunked(4).forEach { rowTypes ->
                                    div("row") {
                                        rowTypes.forEach { typeName ->
                                            div("col") {
                                                +typeName
                                                input(type = InputType.checkBox, classes = "ml-3") {
                                                    attrs.defaultChecked = state.selectedTypes.contains(typeName)
                                                    attrs.onClickFunction = {
                                                        val newSelectedList = state.selectedTypes
                                                        if (state.selectedTypes.contains(typeName)){
                                                            newSelectedList.remove(typeName)
                                                            setState { selectedTypes = newSelectedList }
                                                        } else {
                                                            newSelectedList.add(typeName)
                                                            setState { selectedTypes = newSelectedList }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    button(type = ButtonType.button, classes = "btn btn-primary") {
                        attrs.onClickFunction = { submitExecutionRequest() }
                        +"Run tests now"
                    }
                }
            }) {
                attrs {
                    header = "Run tests"
                    leftBorderColor = "primary"
                }
            }

            child(cardComponent {
                p("small") {
                    +"Name: ${props.executionRequest.project.name}"
                }
                p("small") {
                    +"Description: ${props.executionRequest.project.description}"
                }
                p("small") {
                    a(href = "#/${props.executionRequest.project.owner}/${props.executionRequest.project.name}/history/latest") {
                        +"Latest test execution: N/A"
                    }
                }
                p("small") {
                    a(href = "#/${props.executionRequest.project.owner}/${props.executionRequest.project.name}/history") {
                        +"Execution history"
                    }
                }
            }) {
                attrs {
                    header = "Project info"
                }
            }
        }
    }

    private fun RBuilder.runErrorEmptyTestTypesModal() = modal {
        attrs {
            isOpen = state.isTestTypesEmpty
            contentLabel = "Empty Test types"
        }
        div {
            h2("h3 mb-0 text-gray-800") {
                +"Please select at least one type"
            }
        }
        button(type = ButtonType.button, classes = "btn btn-primary") {
            attrs.onClickFunction = { setState { isTestTypesEmpty = false } }
            +"Close"
        }
    }

    private fun RBuilder.runErrorNotGitUrl() = modal {
        attrs {
            isOpen = state.isNoGitUrl
            contentLabel = "No git url"
        }
        div {
            h2("h3 mb-0 text-gray-800") {
                +"Please provide a git url"
            }
        }
        button(type = ButtonType.button, classes = "btn btn-primary") {
            attrs.onClickFunction = { setState { isNoGitUrl = false } }
            +"Close"
        }
    }

    private fun RBuilder.runErrorBothProjectTypesModal() = modal {
        attrs {
            isOpen = state.isBothProjectOpen
            contentLabel = "Both type of project"
        }
        div {
            h2("h3 mb-0 text-gray-800") {
                +"Please close one of type project"
            }
        }
        button(type = ButtonType.button, classes = "btn btn-primary") {
            attrs.onClickFunction = { setState { isBothProjectOpen = false } }
            +"Close"
        }
    }

    private fun RBuilder.runErrorFromBackendModal() = modal {
        attrs {
            isOpen = state.isErrorFromBackend
            contentLabel = "Error from backend"
        }
        div {
            h2("h3 mb-0 text-gray-800") {
                +state.errorTextFromBackend
            }
        }
        button(type = ButtonType.button, classes = "btn btn-primary") {
            attrs.onClickFunction = { setState { isErrorFromBackend = false } }
            +"Close"
        }
    }

    private fun RBuilder.runErrorNoneTypeProjectModal() = modal {
        attrs {
            isOpen = state.isNoneProjectOpen
            contentLabel = "No type has been selected"
        }
        div {
            h2("h3 mb-0 text-gray-800") {
                +"Please select one of project type"
            }
        }
        button(type = ButtonType.button, classes = "btn btn-primary") {
            attrs.onClickFunction = { setState { isNoneProjectOpen = false } }
            +"Close"
        }
    }

    private fun RBuilder.runErrorNonePropertyFile() = modal {
        attrs {
            isOpen = state.isPropertyFileNotSelect
            contentLabel = "No property file has been selected"
        }
        div {
            h2("h3 mb-0 text-gray-800") {
                +"Please select property file"
            }
        }
        button(type = ButtonType.button, classes = "btn btn-primary") {
            attrs.onClickFunction = { setState { isPropertyFileNotSelect = false } }
            +"Close"
        }
    }

    private fun RBuilder.runErrorNoneBinaryFile() = modal {
        attrs {
            isOpen = state.isBinaryFileNotSelect
            contentLabel = "No binary file has been selected"
        }
        div {
            h2("h3 mb-0 text-gray-800") {
                +"Please select binary file"
            }
        }
        button(type = ButtonType.button, classes = "btn btn-primary") {
            attrs.onClickFunction = { setState { isBinaryFileNotSelect = false } }
            +"Close"
        }
    }
}

/**
 * @return a [Project] constructed from these props
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
fun ProjectExecutionRouteProps.toProject() = Project(
    owner = "owner",  // this.project.owner,
    name = "name",  // this.project.name,
    description = "Todo: fetch description",
    url = "Todo: fetch URL",
)

/**
 * @return git repository
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
fun ProjectExecutionRouteProps.toGitRepository() = GitRepository(url = "gti")  // this.gitRepository.url)

/**
 * @return execution request
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
fun ProjectExecutionRouteProps.toExecutionRequest() = ExecutionRequest(this.toProject(), this.toGitRepository())
