@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.contests

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.externals.markdown.reactMarkdown
import com.saveourtool.save.frontend.utils.*

import csstype.ClassName
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.div

import kotlinx.js.jso

private val columnCard = cardComponent(hasBg = true, isPaddingBottomNull = true)

/**
 * INFO tab in ContestView
 */
val contestInfoMenu = contestInfoMenu()

/**
 * ContestInfoMenu functional component props
 */
external interface ContestInfoMenuProps : Props {
    /**
     * Current contest name
     */
    var contestName: String?
}

/**
 * @return ReactElement
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun contestInfoMenu() = FC<ContestInfoMenuProps> { props ->
    val (contest, setContest) = useState<ContestDto?>(null)
    useRequest {
        val contestDto = get(
            "$apiUrl/contests/${props.contestName}",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<ContestDto>()
            }
        setContest(contestDto)
    }

    div {
        className = ClassName("d-flex justify-content-center")
        div {
            className = ClassName("col-8")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Description"
            }
            div {
                className = ClassName("text-center")
                columnCard {
                    child(reactMarkdown(jso {
                        this.children = contest?.description ?: "No description provided **yet**"
                    }))
                }
            }
        }
    }

    div {
        className = ClassName("mt-4 mb-3")
        div {
            className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
            +"Public tests"
        }
        publicTestComponent {
            this.contestName = props.contestName ?: ""
        }
    }
}
