/**
 * Component for selecting test suites in search mode
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.testsuiteselector

import com.saveourtool.save.frontend.components.basic.showAvaliableTestSuites
import com.saveourtool.save.frontend.externals.lodash.DEFAULT_DEBOUNCE_PERIOD
import com.saveourtool.save.frontend.externals.lodash.debounce
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteFilters

import csstype.ClassName
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val testSuiteSelectorSearchMode = testSuiteSelectorSearchMode()

/**
 * [Props] for [testSuiteSelectorSearchMode] component
 */
external interface TestSuiteSelectorSearchModeProps : Props {
    /**
     * List of test suite ids that should be preselected
     */
    var preselectedTestSuiteIds: List<Long>

    /**
     * Callback invoked when test suite is being removed
     */
    var onTestSuiteIdsUpdate: (List<Long>) -> Unit
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod", "ComplexMethod")
private fun testSuiteSelectorSearchMode() = FC<TestSuiteSelectorSearchModeProps> { props ->
    val (selectedTestSuites, setSelectedTestSuites) = useState<List<TestSuiteDto>>(emptyList())
    val (filteredTestSuites, setFilteredTestSuites) = useState<List<TestSuiteDto>>(emptyList())
    useRequest(isDeferred = false) {
        val testSuitesFromBackend: List<TestSuiteDto> = post(
            url = "$apiUrl/test-suites/get-by-ids",
            headers = jsonHeaders,
            body = Json.encodeToString(props.preselectedTestSuiteIds),
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
            .decodeFromJsonString()
        setSelectedTestSuites(testSuitesFromBackend)
    }()

    val (filters, setFilters) = useState(TestSuiteFilters.empty)
    val getFilteredTestSuites = debounce(
        useRequest(dependencies = arrayOf(filters)) {
            val testSuitesFromBackend: List<TestSuiteDto> = get(
                url = "$apiUrl/test-suites/filtered${filters.toQueryParams()}",
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
                responseHandler = ::noopResponseHandler,
            )
                .decodeFromJsonString()
            setFilteredTestSuites(testSuitesFromBackend)
        },
        DEFAULT_DEBOUNCE_PERIOD,
    )

    useEffect(filters) {
        if (filters.isEmpty()) {
            setFilteredTestSuites(emptyList())
        } else {
            getFilteredTestSuites()
        }
    }

    div {
        className = ClassName("d-flex justify-content-around mb-3")
        input {
            className = ClassName("form-control mr-1")
            value = filters.name
            placeholder = "Name"
            onChange = { event ->
                setFilters { it.copy(name = event.target.value) }
            }
        }
        input {
            className = ClassName("form-control ml-1")
            value = filters.tags
            placeholder = "Tags"
            onChange = { event ->
                setFilters { it.copy(tags = event.target.value) }
            }
        }
    }

    showAvaliableTestSuites(filteredTestSuites, selectedTestSuites) { testSuite ->
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
