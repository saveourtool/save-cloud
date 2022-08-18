/**
 * Component for selecting test suites in browser mode
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.testsuiteselector

import com.saveourtool.save.frontend.components.basic.showAvaliableTestSuites
import com.saveourtool.save.frontend.externals.fontawesome.faCheckDouble
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKeyList
import csstype.ClassName
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.aria.AriaRole
import react.dom.aria.ariaLabel
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.ol
import react.dom.html.ReactHTML.ul
import react.useState

val testSuiteSelectorBrowserMode = testSuiteSelectorBrowserMode()

/**
 * [Props] for [testSuiteSelectorBrowserMode] component
 */
external interface TestSuiteSelectorBrowserModeProps : Props {
    /**
     * Lambda invoked when test suites were successfully set
     */
    var onTestSuiteIdsUpdate: (List<Long>) -> Unit

    /**
     * List of test suite ids that should be preselected
     */
    var preselectedTestSuiteIds: List<Long>

    /**
     * Specific organization name which reduces list of test suites source.
     * If it's null we show public tests
     */
    var specificOrganizationName: String?
}

@Suppress(
    "TOO_MANY_PARAMETERS",
    "TOO_LONG_FUNCTION",
    "LongParameterList",
    "LongMethod"
)
private fun ChildrenBuilder.showBreadcrumb(
    selectedOrganization: String?,
    selectedTestSuiteSource: String?,
    selectedTestSuiteVersion: String?,
    shouldDisplayVersion: Boolean,
    onOrganizationsClick: () -> Unit,
    onSelectedOrganizationClick: () -> Unit,
    onSelectedTestSuiteSourceClick: () -> Unit,
) {
    nav {
        ariaLabel = "breadcrumb"
        ol {
            className = ClassName("breadcrumb")
            li {
                className = ClassName("breadcrumb-item")
                a {
                    role = "button".unsafeCast<AriaRole>()
                    onClick = {
                        selectedOrganization?.let {
                            onOrganizationsClick()
                        }
                    }
                    +"organizations"
                }
            }
            selectedOrganization?.let {
                li {
                    val isActive = selectedTestSuiteSource?.let { "" } ?: "active"
                    className = ClassName("breadcrumb-item $isActive")
                    a {
                        role = "button".unsafeCast<AriaRole>()
                        onClick = {
                            selectedTestSuiteSource?.let {
                                onSelectedOrganizationClick()
                            }
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
                        role = "button".unsafeCast<AriaRole>()
                        onClick = {
                            if (shouldDisplayVersion) {
                                selectedTestSuiteVersion?.let {
                                    onSelectedTestSuiteSourceClick()
                                }
                            }
                        }
                        +selectedTestSuiteSource
                    }
                }
            }
            if (shouldDisplayVersion) {
                selectedTestSuiteVersion?.let {
                    li {
                        a {
                            role = "button".unsafeCast<AriaRole>()
                            className = ClassName("breadcrumb-item active")
                            +selectedTestSuiteVersion
                        }
                    }
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

@Suppress("TOO_LONG_FUNCTION", "LongMethod", "ComplexMethod")
private fun testSuiteSelectorBrowserMode() = FC<TestSuiteSelectorBrowserModeProps> { props ->
    val (selectedOrganization, setSelectedOrganization) = useState<String?>(null)
    val (selectedTestSuiteSource, setSelectedTestSuiteSource) = useState<String?>(null)
    val (selectedTestSuiteVersion, setSelectedTestSuiteVersion) = useState<String?>(null)
    val (selectedTestSuites, setSelectedTestSuites) = useState<List<TestSuiteDto>>(emptyList())

    val (availableOrganizations, setAvailableOrganizations) = useState<List<String>>(emptyList())
    val (availableTestSuiteSources, setAvailableTestSuiteSources) = useState<List<String>>(emptyList())
    useRequest {
        val testSuitesSourcesResponse = props.specificOrganizationName?.let { organizationName ->
            get(
                url = "$apiUrl/test-suites-sources/$organizationName/list",
                headers = jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )
        } ?: run {
            get(
                url = "$apiUrl/test-suites-sources/public-list",
                headers = jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )
        }
        val testSuitesSources = testSuitesSourcesResponse
            .decodeFromJsonString<List<TestSuitesSourceDto>>()
        setAvailableOrganizations(testSuitesSources.map { it.organizationName }.distinct())
        setAvailableTestSuiteSources(testSuitesSources.map { it.name })
    }()

    val (availableTestSuitesVersions, setAvailableTestSuitesVersions) = useState<List<String>>(emptyList())
    useRequest(dependencies = arrayOf(selectedTestSuiteSource)) {
        selectedTestSuiteSource?.let { selectedTestSuiteSource ->
            val testSuiteSourcesVersions: List<String> = get(
                url = "$apiUrl/test-suites-sources/$selectedOrganization/${encodeURIComponent(selectedTestSuiteSource)}/list-snapshot",
                headers = jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )
                .decodeFromJsonString<TestSuitesSourceSnapshotKeyList>()
                .map { it.version }
            setAvailableTestSuitesVersions(testSuiteSourcesVersions)
            setSelectedTestSuiteVersion(testSuiteSourcesVersions.singleOrNull())
        }
    }()

    val (availableTestSuites, setAvailableTestSuites) = useState<List<TestSuiteDto>>(emptyList())
    useRequest(dependencies = arrayOf(selectedTestSuiteVersion)) {
        selectedTestSuiteVersion?.let { selectedTestSuiteVersion ->
            selectedTestSuiteSource?.let { selectedTestSuiteSource ->
                val testSuites: List<TestSuiteDto> = get(
                    url = "$apiUrl/test-suites-sources/$selectedOrganization/${
                        encodeURIComponent(
                            selectedTestSuiteSource
                        )
                    }" +
                            "/get-test-suites?version=${encodeURIComponent(selectedTestSuiteVersion)}",
                    headers = jsonHeaders,
                    loadingHandler = ::noopLoadingHandler,
                    responseHandler = ::noopResponseHandler,
                )
                    .decodeFromJsonString()
                setAvailableTestSuites(testSuites)
                testSuites.filter {
                    it.id in props.preselectedTestSuiteIds
                }
                    .let {
                        setSelectedTestSuites(it)
                    }
            }
        }
    }()

    val (namePrefix, setNamePrefix) = useState("")
    div {
        // ==================== BREADCRUMB ====================
        className = ClassName("")
        showBreadcrumb(
            selectedOrganization,
            selectedTestSuiteSource,
            selectedTestSuiteVersion,
            availableTestSuitesVersions.size > 1,
            {
                setSelectedOrganization(null)
                setSelectedTestSuiteSource(null)
                setSelectedTestSuiteVersion(null)
                setNamePrefix("")
            },
            {
                setSelectedTestSuiteSource(null)
                setSelectedTestSuiteVersion(null)
                setNamePrefix("")
            }
        ) {
            setSelectedTestSuiteVersion(null)
            setNamePrefix("")
        }
        // ==================== TOOLBAR ====================
        div {
            className = ClassName("d-flex justify-content-center mb-2")
            input {
                className = ClassName("form-control")
                value = namePrefix
                placeholder = selectedOrganization?.let {
                    selectedTestSuiteSource?.let {
                        selectedTestSuiteVersion?.let {
                            "Test suite name"
                        } ?: "Test suite version name"
                    } ?: "Test suite source name"
                } ?: "Organization name"
                onChange = {
                    setNamePrefix(it.target.value)
                }
            }
            selectedTestSuiteVersion?.let {
                val active = if (selectedTestSuites.containsAll(availableTestSuites)) {
                    "active"
                } else {
                    ""
                }
                button {
                    className = ClassName("btn btn-outline-secondary $active")
                    onClick = {
                        setSelectedTestSuites { selectedTestSuites ->
                            if (selectedTestSuites.containsAll(availableTestSuites)) {
                                selectedTestSuites.filter { it !in availableTestSuites }
                            } else {
                                selectedTestSuites.toMutableList()
                                    .apply {
                                        addAll(availableTestSuites)
                                    }
                                    .distinctBy { it.id }
                            }
                                .also { testSuites ->
                                    props.onTestSuiteIdsUpdate(testSuites.map { it.requiredId() })
                                }
                        }
                    }
                    fontAwesomeIcon(faCheckDouble)
                }
            }
        }

        // ==================== SELECTOR ====================
        div {
            className = ClassName("")
            when {
                selectedOrganization == null -> showAvaliableOptions(
                    availableOrganizations.filter { it.contains(namePrefix, true) }
                ) { organization ->
                    setSelectedOrganization(organization)
                }
                selectedTestSuiteSource == null -> showAvaliableOptions(
                    availableTestSuiteSources.filter { it.contains(namePrefix, true) }
                ) { testSuiteSource ->
                    setSelectedTestSuiteSource(testSuiteSource)
                }
                selectedTestSuiteVersion == null -> showAvaliableOptions(
                    availableTestSuitesVersions.filter { it.contains(namePrefix, true) }
                ) { testSuiteVersion ->
                    setSelectedTestSuiteVersion(testSuiteVersion)
                }
                else -> showAvaliableTestSuites(
                    availableTestSuites.filter { it.name.contains(namePrefix, true) },
                    selectedTestSuites,
                ) { testSuite ->
                    setSelectedTestSuites { selectedTestSuites ->
                        selectedTestSuites.toMutableList()
                            .apply {
                                if (testSuite in selectedTestSuites) {
                                    remove(testSuite)
                                } else {
                                    add(testSuite)
                                }
                            }
                            .toList()
                            .also { listOfTestSuiteDtos ->
                                props.onTestSuiteIdsUpdate(listOfTestSuiteDtos.map { it.requiredId() })
                            }
                    }
                }
            }
        }
    }
}
