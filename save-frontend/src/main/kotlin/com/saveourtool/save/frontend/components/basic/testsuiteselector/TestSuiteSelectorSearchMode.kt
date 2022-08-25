/**
 * Component for selecting test suites in search mode
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.testsuiteselector

import com.saveourtool.save.frontend.components.basic.showAvaliableTestSuites
import com.saveourtool.save.frontend.externals.lodash.debounce
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteFilters
import com.saveourtool.save.utils.DEFAULT_DEBOUNCE_PERIOD

import csstype.ClassName
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.events.ChangeEvent
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

private fun ChildrenBuilder.buildInput(
    currentValue: String,
    inputName: String,
    classes: String,
    setValue: (ChangeEvent<HTMLInputElement>) -> Unit
) {
    input {
        className = ClassName("form-control $classes")
        value = currentValue
        placeholder = inputName
        onChange = setValue
    }
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod", "ComplexMethod")
private fun testSuiteSelectorSearchMode() = FC<TestSuiteSelectorSearchModeProps> { props ->
    val (selectedTestSuites, setSelectedTestSuites) = useState<List<TestSuiteDto>>(emptyList())
    val (filteredTestSuites, setFilteredTestSuites) = useState<List<TestSuiteDto>>(emptyList())
    useRequest {
        val testSuitesFromBackend: List<TestSuiteDto> = post(
            url = "$apiUrl/test-suites/get-by-ids",
            headers = jsonHeaders,
            body = Json.encodeToString(props.preselectedTestSuiteIds),
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
            .decodeFromJsonString()
        setSelectedTestSuites(testSuitesFromBackend)
    }

    val (filters, setFilters) = useState(TestSuiteFilters.empty)
    val getFilteredTestSuites = debounce(
        useDeferredRequest {
            if (filters.isNotEmpty()) {
                val testSuitesFromBackend: List<TestSuiteDto> = get(
                    url = "$apiUrl/test-suites/filtered${
                        filters.copy(language = encodeURIComponent(filters.language)).toQueryParams()
                    }",
                    headers = jsonHeaders,
                    loadingHandler = ::noopLoadingHandler,
                    responseHandler = ::noopResponseHandler,
                )
                    .decodeFromJsonString()
                setFilteredTestSuites(testSuitesFromBackend)
            }
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
        buildInput(filters.name, "Name", "mr-1") { event ->
            setFilters { it.copy(name = event.target.value) }
        }
        buildInput(filters.language, "Language", "ml-1 mr-1") { event ->
            setFilters { it.copy(language = event.target.value) }
        }
        buildInput(filters.tags, "Tags", "ml-1") { event ->
            setFilters { it.copy(tags = event.target.value) }
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
