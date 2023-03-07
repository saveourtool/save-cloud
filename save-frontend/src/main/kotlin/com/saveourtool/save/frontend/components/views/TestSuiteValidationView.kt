@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.frontend.utils.apiUrl
import com.saveourtool.save.frontend.utils.asMouseEventHandler
import com.saveourtool.save.frontend.utils.useDeferredEffect
import com.saveourtool.save.frontend.utils.useEventStream
import com.saveourtool.save.frontend.utils.useNdjson
import com.saveourtool.save.test.TestSuiteValidationProgress
import csstype.BackgroundColor
import csstype.Border
import csstype.ColorProperty
import csstype.Height
import csstype.MinHeight
import csstype.Width
import js.core.jso
import react.ChildrenBuilder
import react.VFC
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.pre
import react.useState
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val READY = "Ready."

private const val DONE = "Done."

@Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
val testSuiteValidationView: VFC = VFC {
    var errorText by useState<String?>(initialValue = null)

    var rawResponse by useState<String?>(initialValue = null)

    /*
     * When dealing with containers, avoid using `by useState()`.
     */
    val (validationResults, setValidationResults) = useState(initialValue = emptyMap<String, TestSuiteValidationProgress>())

    /**
     * Updates the validation results.
     *
     * @param value the validation result to add to the state of this component.
     */
    operator fun ChildrenBuilder.plusAssign(
        value: TestSuiteValidationProgress
    ) {
        /*
         * When adding items to a container, prefer a lambda form of `StateSetter.invoke()`.
         */
        setValidationResults { oldValidationResults ->
            /*
             * Preserve the order of keys in the map.
             */
            linkedMapOf<String, TestSuiteValidationProgress>().apply {
                putAll(oldValidationResults)
                this[value.checkId] = value
            }
        }
    }

    /**
     * Clears the validation results.
     *
     * @return [Unit]
     */
    fun clearResults() =
            setValidationResults(emptyMap())

    val init = {
        errorText = null
        rawResponse = "Awaiting server response..."
        clearResults()
    }

    div {
        id = "test-suite-validation-status"

        style = jso {
            border = "1px solid f0f0f0".unsafeCast<Border>()
            width = "100%".unsafeCast<Width>()
            height = "100%".unsafeCast<Height>()
            minHeight = "600px".unsafeCast<MinHeight>()
            backgroundColor = "#ffffff".unsafeCast<BackgroundColor>()
        }

        div {
            id = "response-error"

            style = jso {
                border = "1px solid #ffd6d6".unsafeCast<Border>()
                width = "100%".unsafeCast<Width>()
                color = "#f00".unsafeCast<ColorProperty>()
                backgroundColor = "#fff0f0".unsafeCast<BackgroundColor>()
            }

            hidden = errorText == null
            +(errorText ?: "No error")
        }

        button {
            +"Validate test suites (application/x-ndjson)"

            disabled = rawResponse !in arrayOf(null, READY, DONE)

            onClick = useNdjson(
                url = "$apiUrl/a/validate",
                init = init,
                onCompletion = {
                    rawResponse = DONE
                },
                onError = { response ->
                    errorText = "Received HTTP ${response.status} ${response.statusText} from the server"
                }
            ) { validationResult ->
                rawResponse = "Reading server response..."
                this@VFC += Json.decodeFromString<TestSuiteValidationProgress>(validationResult)
            }.asMouseEventHandler()
        }

        button {
            +"Validate test suites (text/event-stream)"

            disabled = rawResponse !in arrayOf(null, READY, DONE)

            onClick = useEventStream(
                url = "$apiUrl/a/validate",
                init = { init() },
                onCompletion = {
                    rawResponse = DONE
                },
                onError = { error, readyState ->
                    errorText = "EventSource error (readyState = $readyState): ${JSON.stringify(error)}"
                },
            ) { validationResult ->
                rawResponse = "Reading server response..."
                this@VFC += Json.decodeFromString<TestSuiteValidationProgress>(validationResult.data.toString())
            }.asMouseEventHandler()
        }

        button {
            +"Clear"

            onClick = useDeferredEffect {
                errorText = null
                rawResponse = null
                clearResults()
            }.asMouseEventHandler()
        }

        pre {
            id = "raw-response"

            +(rawResponse ?: READY)
        }

        testSuiteValidationResultView {
            this.validationResults = validationResults.values
        }
    }
}
