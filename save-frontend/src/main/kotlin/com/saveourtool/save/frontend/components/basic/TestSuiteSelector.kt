/**
 * Component for selecting test suites
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.frontend.components.basic.organizations.encodeURIComponent
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.externals.modal.Classes
import com.saveourtool.save.frontend.externals.modal.CssProperties
import com.saveourtool.save.frontend.externals.modal.Styles
import com.saveourtool.save.frontend.externals.modal.modal
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.permission.SetRoleRequest
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKeyList
import com.saveourtool.save.v1

import csstype.ClassName
import csstype.Width
import kotlinx.coroutines.await
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.*
import react.dom.*
import react.dom.html.InputType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.ul

import kotlinx.js.jso
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.dom.aria.ariaHidden
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.ol
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.small
import kotlin.js.json

/**
 * [Props] for [testSuiteSelector] component
 */
external interface TestSuiteSelectorProps : Props {
    /**
     * Lambda invoked when test suites were successfully set
     */
    var onSuccess: (String) -> Unit

    /**
     * Lambda invoked when an error occurred
     */
    var onFailure: (Response) -> Unit
}

fun ChildrenBuilder.showTestSuiteSelectorModal(
    contestName: String,
    isOpen: Boolean,
    onSuccess: (String) -> Unit,
    onFailure: (Response) -> Unit,
    onClose: () -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = Styles(
            content = json(
                "top" to "15%",
                "left" to "20%",
                "right" to "20%",
                "bottom" to "5%",
                "position" to "absolute",
                "overflow" to "hide"
            ).unsafeCast<CssProperties>()
        )
        div {
            className = ClassName("modal-dialog modal-dialog-scrollable")
            div {
                className = ClassName("modal-content")
                div {
                    className = ClassName("modal-header")
                    h5 {
                        +"Test suite selector"
                    }
                    button {
                        type = ButtonType.button
                        className = ClassName("close")
                        asDynamic()["data-dismiss"] = "modal"
                        ariaLabel = "Close"
                        span {
                            ariaHidden = true
                            +"x"
                        }
                        onClick = {
                            onClose()
                        }
                    }
                }

                div {
                    className = ClassName("modal-body")
                    testSuiteSelector {
                        this.onSuccess = onSuccess
                        this.onFailure = onFailure
                    }
                }

                div {
                    className = ClassName("modal-footer")
                    div {
                        className = ClassName("d-flex justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-secondary mt-4")
                            +"Cancel"
                            onClick = {
                                onClose()
                            }
                        }
                    }
                }
            }
        }
    }
}

val testSuiteSelector = testSuiteSelector()

private fun testSuiteSelector() = FC<TestSuiteSelectorProps> { props ->
    val (selectedOrganization, setSelectedOrganization) = useState<String?>(null)
    val (selectedTestSuiteSource, setSelectedTestSuiteSource) = useState<String?>(null)
    val (selectedTestSuiteVersion, setSelectedTestSuiteVersion) = useState<String?>(null)
    val (selectedTestSuites, setSelectedTestSuites) = useState<List<TestSuiteDto>>(emptyList())

    val (avaliableOrganizations, setAvaliableOrganizations) = useState<List<String>>(emptyList())
    useRequest(dependencies = arrayOf(selectedOrganization)) {
        val organizations = get(
            url = "$apiUrl/test-suites-sources/organizations-list",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
            .decodeFromJsonString<List<String>>()
        setAvaliableOrganizations(organizations)
    }()

    val (avaliableTestSuiteSources, setAvaliableTestSuiteSources) = useState<List<String>>(emptyList())
    useRequest(dependencies = arrayOf(selectedOrganization)) {
        selectedOrganization?.let { selectedOrganization ->
            val testSuiteSources = get(
                url = "$apiUrl/test-suites-sources/$selectedOrganization/list",
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
                responseHandler = ::noopResponseHandler,
            )
                .decodeFromJsonString<List<TestSuitesSourceDto>>()
                .map { it.name }
            setAvaliableTestSuiteSources(testSuiteSources)
        }
    }()

    val (avaliableTestSuitesVersions, setAvaliableTestSuitesVersions) = useState<List<String>>(emptyList())
    useRequest(dependencies = arrayOf(selectedTestSuiteSource)) {
        selectedTestSuiteSource?.let { selectedTestSuiteSource ->
            val testSuiteSourcesVersions = get(
                url = "$apiUrl/test-suites-sources/${selectedOrganization}/${encodeURIComponent(selectedTestSuiteSource)}/list-snapshot",
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
            )
                .decodeFromJsonString<TestSuitesSourceSnapshotKeyList>()
                .map { it.version }
            setAvaliableTestSuitesVersions(testSuiteSourcesVersions)
        }
    }()

    val (avaliableTestSuites, setAvaliableTestSuites) = useState<List<TestSuiteDto>>(emptyList())
    useRequest(dependencies = arrayOf(selectedTestSuiteVersion)) {
        selectedTestSuiteVersion?.let { selectedTestSuiteVersion ->
            selectedTestSuiteSource?.let { selectedTestSuiteSource ->
                val testSuites = get(
                    url = "$apiUrl/test-suites-sources/${selectedOrganization}/${
                        encodeURIComponent(
                            selectedTestSuiteSource
                        )
                    }" +
                            "/get-test-suites?version=${encodeURIComponent(selectedTestSuiteVersion)}",
                    headers = jsonHeaders,
                    loadingHandler = ::loadingHandler,
                )
                    .decodeFromJsonString<List<TestSuiteDto>>()
                setAvaliableTestSuites(testSuites)
            }
        }
    }()

    val sendSetContestTestSuitesRequest = useRequest(isDeferred = false) {
        if (selectedTestSuites.isNotEmpty()) {
            val response = post(
                url = "$apiUrl/contests/update",
                headers = jsonHeaders,
                body = undefined,
                loadingHandler = ::loadingHandler,
                responseHandler = ::noopResponseHandler,
            )
            if (response.ok) {
                props.onSuccess(response.text().await())
            } else {
                props.onFailure(response)
            }
        }
    }

    div {
        // ==================== BREADCRUMB ====================
        className = ClassName("")
        nav {
            ariaLabel = "breadcrumb"
            ol {
                className = ClassName("breadcrumb")
                li {
                    className = ClassName("breadcrumb-item")
                    a {
                        onClick = {
                            setSelectedOrganization(null)
                            setSelectedTestSuiteSource(null)
                            setSelectedTestSuiteVersion(null)
                        }
                        +"organizations"
                    }
                }
                selectedOrganization?.let {
                    li {
                        val isActive = selectedTestSuiteSource?.let { "" } ?: "active"
                        className = ClassName("breadcrumb-item $isActive")
                        a {
                            onClick = {
                                setSelectedTestSuiteSource(null)
                                setSelectedTestSuiteVersion(null)
                            }
                            +selectedOrganization
                        }
                    }
                }
                selectedTestSuiteSource?.let {
                    li {
                        val isActive = selectedTestSuiteVersion?.let { "" } ?: "active"
                        className = ClassName("breadcrumb-item $isActive")
                        a {
                            onClick = {
                                setSelectedTestSuiteVersion(null)
                            }
                            +selectedTestSuiteSource
                        }
                    }
                }
                selectedTestSuiteVersion?.let {
                    li {
                        className = ClassName("breadcrumb-item active")
                        +selectedTestSuiteVersion
                    }
                }
            }
        }
        // ==================== SELECTOR ====================
        div {
            className = ClassName("")
            when {
                selectedOrganization == null -> showAvaliableOptions(avaliableOrganizations) { organization ->
                    setSelectedOrganization(organization)
                }
                selectedTestSuiteSource == null -> showAvaliableOptions(avaliableTestSuiteSources) { testSuiteSource ->
                    setSelectedTestSuiteSource(testSuiteSource)
                }
                selectedTestSuiteVersion == null -> showAvaliableOptions(avaliableTestSuitesVersions) { testSuiteVersion ->
                    setSelectedTestSuiteVersion(testSuiteVersion)
                }
                else -> showAvaliableTestSuites(avaliableTestSuites, selectedTestSuites) { testSuite ->
                    setSelectedTestSuites { selectedTestSuites ->
                        selectedTestSuites.toMutableList().apply {
                            if (testSuite in selectedTestSuites) {
                                remove(testSuite)
                            } else {
                                add(testSuite)
                            }
                        }.toList()
                    }
                }
            }
        }
        div {
            className = ClassName("")
            button {
                type = ButtonType.button
                className = ClassName("btn btn-secondary mt-4")
                +"Cancel"
                onClick = {
                    sendSetContestTestSuitesRequest()
                }
            }
        }
    }
}

private fun ChildrenBuilder.showAvaliableOptions(
    options: List<String>,
    onOptionClick: (String) -> Unit,
) {
    ul {
        className = ClassName("list-group")
        options.forEach { option ->
            li {
                className = ClassName("list-group-item")
                onClick = {
                    onOptionClick(option)
                }
                +option
            }
        }
    }
}

private fun ChildrenBuilder.showAvaliableTestSuites(
    testSuites: List<TestSuiteDto>,
    selectedTestSuites: List<TestSuiteDto>,
    onTestSuiteClick: (TestSuiteDto) -> Unit,
) {
    div {
        className = ClassName("list-group")
        testSuites.forEach { testSuite ->
            val active = if (testSuite in selectedTestSuites) { "active" } else { "" }
            a {
                className = ClassName("list-group-item list-group-item-action $active")
                onClick = {
                    onTestSuiteClick(testSuite)
                }
                div {
                    className = ClassName("d-flex w-100 justify-content-between")
                    h5 {
                        className = ClassName("mb-1")
                        +(testSuite.name)
                    }
                    small {
                        +(testSuite.language ?: "")
                    }
                }
                p {
                    +(testSuite.description ?: "")
                }
                small {
                    +(testSuite.tags?.joinToString(", ") ?: "")
                }
            }
        }
    }
}
