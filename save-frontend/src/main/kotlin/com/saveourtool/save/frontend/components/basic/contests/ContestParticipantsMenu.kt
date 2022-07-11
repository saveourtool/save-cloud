@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.contests

import com.saveourtool.save.entities.ContestResult
import com.saveourtool.save.frontend.components.basic.projectScoreCard
import com.saveourtool.save.frontend.utils.*

import csstype.*
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div

import kotlinx.js.jso

private val projectScoreCardComponent = projectScoreCard()

/**
 * ContestParticipantsMenu component props
 */
external interface ContestParticipantsMenuProps : Props {
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
fun contestParticipantsMenu() = FC<ContestParticipantsMenuProps> { props ->
    val (results, setResults) = useState<List<ContestResult>>(emptyList())
    useRequest(isDeferred = false) {
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
    }()

    div {
        className = ClassName("mb-3")
        style = jso {
            justifyContent = JustifyContent.center
            display = Display.flex
            flexDirection = FlexDirection.column
            alignItems = AlignItems.center
        }
        results.forEach { contestResult ->
            div {
                className = ClassName("col-10 mb-2")
                a {
                    href = "#/${contestResult.organizationName}/${contestResult.projectName}"
                    className = ClassName("stretched-link")
                }
                child(projectScoreCardComponent, jso {
                    projectName = "${contestResult.organizationName}/${contestResult.projectName}"
                    contestScore = contestResult.score.toDouble()
                })
            }
        }
    }
}
