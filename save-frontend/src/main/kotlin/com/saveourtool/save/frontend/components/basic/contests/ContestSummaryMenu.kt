@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.contests

import com.saveourtool.save.entities.ContestResult
import com.saveourtool.save.frontend.components.basic.scoreCard
import com.saveourtool.save.frontend.utils.*

import csstype.*
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6

import kotlinx.js.jso

/**
 * SUMMARY tab in ContestView
 */
val contestSummaryMenu = contestSummaryMenu()

/**
 * ContestSummaryMenu component [Props]
 */
external interface ContestSummaryMenuProps : Props {
    /**
     * Name of a current contest
     */
    var contestName: String
}

/**
 * @return ReactElement
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "AVOID_NULL_CHECKS"
)
private fun contestSummaryMenu() = FC<ContestSummaryMenuProps> { props ->
    val (results, setResults) = useState<List<ContestResult>>(emptyList())
    useRequest {
        val projectResults = get(
            url = "$apiUrl/contests/${props.contestName}/scores",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<Array<ContestResult>>()
            }
            .sortedByDescending { it.score }
        setResults(projectResults)
    }
    div {
        className = ClassName("mb-3")
        style = jso {
            justifyContent = JustifyContent.center
            display = Display.flex
            flexDirection = FlexDirection.column
            alignItems = AlignItems.center
        }
        if (results.isEmpty()) {
            h6 {
                className = ClassName("text-center")
                +"There are no participants yet. You can be the first one to participate in it!"
            }
        }
        results.forEach { contestResult ->
            div {
                className = ClassName("col-10 mb-2")
                a {
                    href = "#/${contestResult.organizationName}/${contestResult.projectName}"
                    className = ClassName("stretched-link")
                }
                scoreCard {
                    name = "${contestResult.organizationName}/${contestResult.projectName}"
                    contestScore = contestResult.score ?: 0.0
                }
            }
        }
    }
}
