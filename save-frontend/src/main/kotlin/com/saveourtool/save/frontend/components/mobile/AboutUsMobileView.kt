/**
 * View with some info about core team
 */

package com.saveourtool.save.frontend.components.mobile

import com.saveourtool.save.frontend.components.views.AboutUsView
import com.saveourtool.save.frontend.components.views.Developer
import com.saveourtool.save.frontend.externals.fontawesome.faGithub
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.externals.markdown.reactMarkdown

import csstype.*
import js.core.jso
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.img

/**
 * A component representing "About us" page
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class AboutUsMobileView : AboutUsView() {
    override fun ChildrenBuilder.render() {
        renderViewHeader()
        renderSaveourtoolInfo()
        renderDevelopers(NUMBER_OF_COLUMNS)
    }

    override fun ChildrenBuilder.renderSaveourtoolInfo() {
        div {
            div {
                className = ClassName("mt-3 d-flex justify-content-center align-items-center")
                div {
                    className = ClassName("col-11")
                    infoCard {
                        div {
                            className = ClassName("justify-content-center")
                            div {
                                className = ClassName("row justify-content-center")
                                img {
                                    src = "${GITHUB_AVATAR_LINK}saveourtool?size=$DEFAULT_AVATAR_SIZE"
                                    className = ClassName("img-fluid mt-auto mb-auto")
                                }
                            }
                            div {
                                className = ClassName("row justify-content-center")
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

    override fun ChildrenBuilder.renderDeveloperCard(developer: Developer) {
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
                    h6 {
                        className = ClassName("d-flex justify-content-center text-center")
                        +developer.name
                    }
                    a {
                        className = ClassName("d-flex justify-content-center")
                        href = "$GITHUB_LINK${developer.githubNickname}"
                        fontAwesomeIcon(faGithub)
                    }
                }
            }
        }
    }

    companion object {
        private const val NUMBER_OF_COLUMNS = 2
    }
}
