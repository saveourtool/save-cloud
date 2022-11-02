/**
 * View with some info about core team
 */

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.utils.get

import csstype.*
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.h5
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
        Developer("Vladislav Frolov", "Cheshiriks"),
        Developer("Peter Trifanov", "petertrr"),
        Developer("Andrey Shcheglov", "0x6675636b796f75676974687562"),
        Developer("Alex Frolov", "sanyavertolet"),
        Developer("Andrey Kuleshov", "akuleshov7"),
        Developer("Nariman Abdullin", "nulls"),
        Developer("Alexey Votintsev", "Arrgentum"),
        Developer("Kirill Gevorkyan", "kgevorkyan"),
        Developer("Dmitriy Morozovsky", "icemachined"),
    ).sortedBy { it.githubNickname }
    private val devCard = cardComponent(hasBg = true)

    override fun ChildrenBuilder.render() {
        renderViewHeader()
        div {
            className = ClassName("mt-3")
            style = jso {
                justifyContent = JustifyContent.spaceAround
                display = Display.flex
                alignItems = AlignItems.center
            }
            renderDevelopers()
        }
    }

    private fun ChildrenBuilder.renderViewHeader() {
        h2 {
            className = ClassName("text-center mt-3")
            style = jso {
                color = Color("#FFFFFF")
            }
            +"saveourtool"
        }
        h4 {
            className = ClassName("text-center")
            +"core team"
        }
    }

    @Suppress("MAGIC_NUMBER")
    private fun ChildrenBuilder.renderDevelopers() {
        div {
            for (rowIndex in 0..2) {
                div {
                    className = ClassName("row")
                    for (colIndex in 0..2) {
                        div {
                            style = jso {
                                alignItems = AlignItems.stretch
                            }
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

    private fun ChildrenBuilder.renderDeveloperCard(developer: Developer) {
        devCard {
            div {
                className = ClassName("p-2")
                div {
                    style = jso {
                        display = Display.flex
                        justifyContent = JustifyContent.center
                    }
                    img {
                        src = "$GITHUB_AVATAR_LINK${developer.githubNickname}?size=$DEFAULT_AVATAR_SIZE"
                        className = ClassName("img-fluid border border-dark rounded-circle m-0")
                    }
                }
                div {
                    className = ClassName(" mt-2")
                    h5 {
                        style = jso {
                            display = Display.flex
                            justifyContent = JustifyContent.center
                        }
                        +developer.name
                    }
                    a {
                        style = jso {
                            display = Display.flex
                            justifyContent = JustifyContent.center
                        }
                        href = "$GITHUB_LINK${developer.githubNickname}"
                        +developer.githubNickname
                    }
                }
            }
        }
    }

    companion object :
        RStatics<AboutUsViewProps, AboutUsViewState, AboutUsView, Context<RequestStatusContext>>(AboutUsView::class) {
        private const val DEFAULT_AVATAR_SIZE = "200"
        private const val GITHUB_AVATAR_LINK = "https://avatars.githubusercontent.com/"
        private const val GITHUB_LINK = "https://github.com/"
        init {
            ContestView.contextType = requestStatusContext
        }
    }
}

/**
 * @property name
 * @property githubNickname
 */
data class Developer(
    val name: String,
    val githubNickname: String,
)
