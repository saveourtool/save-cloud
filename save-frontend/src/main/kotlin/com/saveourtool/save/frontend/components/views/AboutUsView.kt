/**
 * View with some info about core team
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.externals.markdown.reactMarkdown

import csstype.*
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.img

import kotlinx.js.jso

/**
 * [Props] of [AboutUsView]
 */
external interface AboutUsViewProps : Props

/**
 * [State] of [AboutUsView]
 */
external interface AboutUsViewState : State

/**
 * A component representing "About us" page
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class AboutUsView : AbstractView<AboutUsViewProps, AboutUsViewState>(true) {
    private val developers = listOf(
        Developer("Vladislav Frolov", "Cheshiriks", "Fullstack"),
        Developer("Peter Trifanov", "petertrr", "Fullstack"),
        Developer("Andrey Shcheglov", "0x6675636b796f75676974687562", "Devops"),
        Developer("Alexander Frolov", "sanyavertolet", "Frontend"),
        Developer("Andrey Kuleshov", "akuleshov7", "Team leader"),
        Developer("Nariman Abdullin", "nulls", "Fullstack"),
        Developer("Alexey Votintsev", "Arrgentum", "Frontend"),
        Developer("Kirill Gevorkyan", "kgevorkyan", "Backend"),
        Developer("Dmitriy Morozovsky", "icemachined", "Backend"),
    ).sortedBy { it.name }
    private val devCard = cardComponent(hasBg = true, isPaddingBottomNull = true)

    override fun ChildrenBuilder.render() {
        renderViewHeader()
        renderSaveourtoolInfo()
        renderDevelopers()
    }

    private fun ChildrenBuilder.renderViewHeader() {
        h2 {
            className = ClassName("text-center mt-3")
            style = jso {
                color = Color("#FFFFFF")
            }
            +"About us"
        }
    }

    private fun ChildrenBuilder.renderSaveourtoolInfo() {
        div {
            div {
                className = ClassName("mt-3 d-flex justify-content-center align-items-center")
                div {
                    className = ClassName("col-6")
                    devCard {
                        div {
                            className = ClassName("m-2 d-flex justify-content-around align-items-center")
                            div {
                                className = ClassName("m-2 d-flex align-items-center align-self-stretch flex-column")
                                img {
                                    src = "${GITHUB_AVATAR_LINK}saveourtool?size=$DEFAULT_AVATAR_SIZE"
                                    className = ClassName("img-fluid mt-auto mb-auto")
                                }
                                a {
                                    className = ClassName("text-center mt-auto mb-2 align-self-end")
                                    href = "mailto:$SAVEOURTOOL_EMAIL"
                                    +SAVEOURTOOL_EMAIL
                                }
                            }
                            child(
                                reactMarkdown(
                                    jso {
                                        this.children = saveourtoolDescription
                                        this.className = "flex-wrap"
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    @Suppress("MAGIC_NUMBER")
    private fun ChildrenBuilder.renderDevelopers() {
        div {
            h4 {
                className = ClassName("text-center mb-2 mt-3")
                +"core team"
            }
            div {
                className = ClassName("mt-3 d-flex justify-content-around align-items-center")
                div {
                    for (rowIndex in 0..2) {
                        div {
                            className = ClassName("row")
                            for (colIndex in 0..2) {
                                div {
                                    className = ClassName("col-4 p-2")
                                    developers.getOrNull(3 * rowIndex + colIndex)?.let {
                                        renderDeveloperCard(it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ChildrenBuilder.renderDeveloperCard(developer: Developer) {
        devCard {
            div {
                className = ClassName("p-3")
                div {
                    className = ClassName("d-flex justify-content-center")
                    img {
                        src = "$GITHUB_AVATAR_LINK${developer.githubNickname}?size=$DEFAULT_AVATAR_SIZE"
                        className = ClassName("img-fluid border border-dark rounded-circle m-0")
                    }
                }
                div {
                    className = ClassName("mt-2")
                    h5 {
                        className = ClassName("d-flex justify-content-center")
                        +developer.name
                    }
                    h6 {
                        className = ClassName("text-center")
                        +developer.description
                    }
                    a {
                        className = ClassName("d-flex justify-content-center")
                        href = "$GITHUB_LINK${developer.githubNickname}"
                        +developer.githubNickname.take(MAX_NICKNAME_LENGTH)
                    }
                }
            }
        }
    }

    companion object :
        RStatics<AboutUsViewProps, AboutUsViewState, AboutUsView, Context<RequestStatusContext>>(AboutUsView::class) {
        private const val DEFAULT_AVATAR_SIZE = "175"
        private const val GITHUB_AVATAR_LINK = "https://avatars.githubusercontent.com/"
        private const val GITHUB_LINK = "https://github.com/"
        private const val MAX_NICKNAME_LENGTH = 15
        private const val SAVEOURTOOL_EMAIL = "saveourtool@gmail.com"
        private val saveourtoolDescription = """
            # Save Our Tool!
    
            Our organization is mostly focused on Static Analysis tools and the eco-system related to such kind of tools.
            We love Kotlin and mostly everything we develop is connected with Kotlin JVM, Kotlin JS or Kotlin Native.
    
            ### Main Repositories:
            - [diktat](${GITHUB_LINK}saveourtool/diktat) - Automated code checker&fixer for Kotlin
            - [save-cli](${GITHUB_LINK}saveourtool/save-cli) - Unified test framework for Static Analyzers and Compilers
            - [save-cloud](${GITHUB_LINK}saveourtool/save-cloud) - Cloud eco-system for CI/CD and benchmarking of Static Analyzers
            - [awesome-benchmarks](${GITHUB_LINK}saveourtool/awesome-benchmarks) - Curated list of benchmarks for different types of testing
    
        """.trimIndent()
        init {
            ContestView.contextType = requestStatusContext
        }
    }
}

/**
 * @property name developer's name
 * @property githubNickname nickname of developer on GitHub
 * @property description brief developer description
 */
data class Developer(
    val name: String,
    val githubNickname: String,
    val description: String = "",
)
