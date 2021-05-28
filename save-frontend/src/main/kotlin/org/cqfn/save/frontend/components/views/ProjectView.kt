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
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.child
import react.dom.a
import react.dom.button
import react.dom.div
import react.dom.h1
import react.dom.h2
import react.dom.h6
import react.dom.input
import react.dom.p
import react.setState

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.role

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

    /**
     * Flag to open test suites
     */
    var isTestTypesOpen: Boolean

    /**
     * Whether error is both types of project were openned
     */
    var isBothProjectOpen: Boolean

    /**
     * Whether error if testSuites empty
     */
    var isTestTypesEmpty: Boolean

    /**
     * Whether error non project types
     */
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
     * List of selected test suites
     */
    var selectedTypes: MutableList<String>
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
    private var pathToPropertyFile: String? = null
    private var gitUrl: String? = null
        get() = props.executionRequest.gitRepository.url
    private var binaryFile: File? = null
    private var propertyFile: File? = null
    private val testTypesList = listOf("Class", "Comment", "Function", "Variable", "Enum", "Space", "Style", "Another")
    private var isProjectAsGitOpen = true
    private var isProjectAsBinaryFile = true
    private lateinit var responseFromExecutionRequest: Response

    init {
        state.isErrorFromBackend = false
        state.isTestTypesOpen = false
        state.isBothProjectOpen = false
        state.isNoneProjectOpen = false
        state.isTestTypesEmpty = false
        state.isBinaryFileNotSelect = false
        state.isPropertyFileNotSelect = false
        state.selectedTypes = mutableListOf()
        state.errorTextFromBackend = ""
    }

    private fun submitExecutionRequest() {
        if (isProjectAsBinaryFile && isProjectAsGitOpen) {
            setState { isBothProjectOpen = true }
            return
        } else if (!isProjectAsBinaryFile && !isProjectAsGitOpen) {
            setState { isNoneProjectOpen = true }
            return
        }
        if (isProjectAsGitOpen) {
            submitExecutionRequestGit()
        } else {
            if (state.selectedTypes.isEmpty()) {
                setState { isTestTypesEmpty = true }
                return
            }
            binaryFile ?: run {
                setState { isBinaryFileNotSelect = true }
                return
            }
            propertyFile ?: run {
                setState { isPropertyFileNotSelect = true }
                return
            }
            submitExecutionRequestBinFile()
        }
    }

    private fun submitExecutionRequestBinFile() {
        val headers = Headers().also {
            it.set("Accept", "application/json")
        }
        val formData = FormData()
        val request = ExecutionRequestForStandardSuites(props.executionRequest.project, state.selectedTypes)
        console.log(JSON.stringify(request))
        formData.append("execution", Blob(arrayOf(JSON.stringify(request)), BlobPropertyBag("application/json")))
        binaryFile?.let { formData.append("property", it) }
        binaryFile?.let { formData.append("binFile", it) }
        submitRequest("submitExecutionRequestBin", headers, formData)
    }

    private fun submitExecutionRequestGit() {
        val executionRequest = ExecutionRequest(
            project = props.executionRequest.project,
            gitRepository = props.executionRequest.gitRepository,
            propertiesRelativePath = pathToPropertyFile ?: "save.properties"
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
        runTestTypesModal()
        runErrorFromBackendModal()
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
                        a(href = "#collapseCardGitProject", classes = "d-block card-header py-3") {
                            attrs["data-toggle"] = "collapse"
                            attrs["aria-expanded"] = false
                            attrs["aria-controls"] = "collapseCardGitProject"
                            attrs {
                                role = "button"
                                onClickFunction = { isProjectAsGitOpen = !isProjectAsGitOpen }
                            }
                            h6("m-0 font-weight-bold text-primary") {
                                +"Upload project as git url"
                            }
                        }
                        // Collapse card to load git url
                        div(classes = "collapse show") {
                            attrs.id = "collapseCardGitProject"
                            div("card-body") {
                                div("pb-3") {
                                    div("d-inline-block") {
                                        h6(classes = "d-inline") {
                                            +"Git url: "
                                        }
                                    }
                                    div("d-inline-block ml-2") {
                                        input(type = InputType.text) {
                                            attrs {
                                                gitUrl?.let {
                                                    value = it
                                                } ?: run {
                                                    placeholder = "https://github.com/"
                                                }
                                                onChangeFunction = {
                                                    val target = it.target as HTMLInputElement
                                                    gitUrl = target.value
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
                                        input(type = InputType.text) {
                                            attrs {
                                                placeholder = "save.properties"
                                                onChangeFunction = {
                                                    val target = it.target as HTMLInputElement
                                                    pathToPropertyFile = target.value
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
                        a(href = "#collapseCardBinFiles", classes = "d-block card-header py-3") {
                            attrs["data-toggle"] = "collapse"
                            attrs["aria-expanded"] = "false"
                            attrs["aria-controls"] = "collapseCardBinFiles"
                            attrs {
                                role = "button"
                                onClickFunction = { isProjectAsBinaryFile = !isProjectAsBinaryFile }
                            }
                            h6("m-0 font-weight-bold text-primary") {
                                +"Upload project as binary file"
                            }
                        }
                        div(classes = "collapse show") {
                            attrs.id = "collapseCardBinFiles"
                            div("card-body") {
                                div("mb-3") {
                                    h6(classes = "d-inline mr-3") {
                                        +"Binary file: "
                                    }
                                    input(type = InputType.file) {
                                        attrs {
                                            onChangeFunction = { event ->
                                                val target = event.target as HTMLInputElement
                                                binaryFile = target.files?.let { it[0] }
                                            }
                                        }
                                    }
                                }
                                div {
                                    h6(classes = "d-inline mr-3") {
                                        +"Properties : "
                                    }
                                    input(type = InputType.file) {
                                        attrs {
                                            onChangeFunction = { event ->
                                                val target = event.target as HTMLInputElement
                                                propertyFile = target.files?.let { it[0] }
                                            }
                                        }
                                    }
                                }
                                button(type = ButtonType.button, classes = "btn btn-primary mt-3") {
                                    attrs.onClickFunction = { setState { isTestTypesOpen = true } }
                                    +"Select test types"
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

    private fun RBuilder.runTestTypesModal() = modal {
        attrs {
            isOpen = state.isTestTypesOpen
            contentLabel = "Select test types"
        }
        div {
            testTypesList.forEach { type ->
                div("mb-3") {
                    +type
                    input(type = InputType.checkBox, classes = "ml-3") {
                        attrs.onClickFunction = {
                            val copyOfTypes = state.selectedTypes
                            if (!copyOfTypes.contains(type)) {
                                copyOfTypes.add(type)
                            } else {
                                copyOfTypes.remove(type)
                            }
                            setState {
                                selectedTypes = copyOfTypes
                            }
                        }
                    }
                }
            }
        }
        button(type = ButtonType.button, classes = "btn btn-primary") {
            attrs.onClickFunction = { setState { isTestTypesOpen = false } }
            +"Close"
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
