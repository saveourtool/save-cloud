@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.contests

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.externals.markdown.reactMarkdown
import com.saveourtool.save.frontend.externals.markdown.rehype.rehypeHighlightPlugin
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.test.TestFilesContent

import csstype.ClassName
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.div

import kotlinx.js.jso

private val columnCard = cardComponent(hasBg = true, isPaddingBottomNull = true)

private val publicTestCard = cardComponent(hasBg = true, isBordered = true, isPaddingBottomNull = true)

/**
 * ContestInfoMenu functional component props
 */
external interface ContestInfoMenuProps : Props {
    /**
     * Current contest name
     */
    var contestName: String?
}

private fun ChildrenBuilder.displayTestLines(header: String, lines: List<String>, language: String? = null) = div {
    div {
        className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
        +header
    }
    val reactMarkdownOptions: dynamic = jso {
        this.children = wrapTestLines(lines, language)
        this.rehypePlugins = arrayOf(::rehypeHighlightPlugin)
    }
    publicTestCard {
        child(reactMarkdown(reactMarkdownOptions))
    }
}

/**
 * @return ReactElement
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun contestInfoMenu(
) = FC<ContestInfoMenuProps> { props ->

    val (contest, setContest) = useState<ContestDto?>(null)
    useRequest(isDeferred = false) {
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
    }()

    val (publicTest, setPublicTest) = useState(TestFilesContent(emptyList(), null))
    useRequest(isDeferred = false) {
        val publicTestDto = get(
            "$apiUrl/contests/${props.contestName}/public-test",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<TestFilesContent>()
            }
        setPublicTest(publicTestDto)
    }()

    div {
        className = ClassName("d-flex justify-content-around mb-3")
        div {
            className = ClassName("col-5")
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
        div {
            className = ClassName("col-5")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Public tests"
            }
            div {
                if (publicTest.testLines.isEmpty()) {
                    div {
                        className = ClassName("text-center")
                        +"Public tests are not provided"
                    }
                } else {
                    columnCard {
                        div {
                            className = ClassName("ml-2 mr-2")
                            div {
                                className = ClassName("mt-3 mb-3")
                                displayTestLines("Test", publicTest.testLines, "kotlin")
                            }
                            publicTest.expectedLines?.let {
                                div {
                                    className = ClassName("mt-3 mb-2")
                                    displayTestLines("Expected", it, "kotlin")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun wrapTestLines(testLines: List<String>, language: String?) = """
    |```${ language ?: "" }
    |${testLines.joinToString("\n")}
    |```""".trimMargin()