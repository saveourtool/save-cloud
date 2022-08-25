/**
 * Component for selecting test suites in manager mode
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.testsuiteselector

import com.saveourtool.save.frontend.components.basic.showAvaliableTestSuites
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.testsuite.TestSuiteDto

import csstype.ClassName
import react.FC
import react.Props
import react.dom.html.ReactHTML.h6
import react.useState

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val testSuiteSelectorManagerMode = testSuiteSelectorManagerMode()

/**
 * [Props] for [testSuiteSelectorManagerMode] component
 */
external interface TestSuiteSelectorManagerModeProps : Props {
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
private fun testSuiteSelectorManagerMode() = FC<TestSuiteSelectorManagerModeProps> { props ->
    val (selectedTestSuites, setSelectedTestSuites) = useState<List<TestSuiteDto>>(emptyList())
    val (preselectedTestSuites, setPreselectedTestSuites) = useState<List<TestSuiteDto>>(emptyList())
    useRequest {
        val testSuitesFromBackend: List<TestSuiteDto> = post(
            url = "$apiUrl/test-suites/get-by-ids",
            headers = jsonHeaders,
            body = Json.encodeToString(props.preselectedTestSuiteIds),
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        )
            .decodeFromJsonString()
        setPreselectedTestSuites(testSuitesFromBackend)
        setSelectedTestSuites(testSuitesFromBackend)
    }

    if (preselectedTestSuites.isEmpty()) {
        h6 {
            className = ClassName("text-center")
            +"No test suites are selected yet."
        }
    } else {
        showAvaliableTestSuites(preselectedTestSuites, selectedTestSuites) { testSuite ->
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
