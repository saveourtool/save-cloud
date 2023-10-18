/**
 * Main view for Demos
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import js.core.jso
import react.FC
import react.Props

import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main
import react.router.useNavigate
import react.useEffect
import web.cssom.*
import kotlinx.browser.window

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
val indexView: FC<IndexViewProps> = FC { props ->
    val navigate = useNavigate()
    useEffect {
        if (window.location.run { hostname in setOf("cosv.dev", "cosv.gitlink.org.cn") && pathname == "/" }) {
            navigate("/vuln")
        }
    }
    useBackground(Style.INDEX)
    particles()
    main {
        className = ClassName("main-content")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("row justify-content-center")
                div {
                    className = ClassName("col-8 card border-secondary lg-shadow shadow-box")
                    div {
                        className = ClassName("row")
                        div {
                            className = ClassName("col")
                            @Suppress("MAGIC_NUMBER")
                            style = jso {
                                background = "rgb(0,0,0) url(img/logo-bg-p-3.png)".unsafeCast<Background>()
                                backgroundRepeat = "no-repeat".unsafeCast<BackgroundRepeat>()
                                backgroundSize = "100% auto".unsafeCast<BackgroundSize>()
                                // need to hardcode the height, as it is very tightly linked to the size of the img
                                // and to logo alignments
                                height = 33.rem
                            }
                            logoButtons { }
                        }
                    }

                    div {
                        className = ClassName("row d-flex justify-content-center")
                        div {
                            className = ClassName("col min-vh-100")
                            style = jso {
                                background = INDEX_VIEW_CUSTOM_BG.unsafeCast<Background>()
                            }

                            props.userInfo
                                ?: run {
                                    separator { }
                                    indexAuth { props.userInfo }
                                }

                            indexViewInfo { userInfo = props.userInfo }
                        }
                    }
                }
            }
        }
    }

    div {
        className = ClassName("row text-center")
        div {
            className = ClassName("col-3 text-center")
        }
    }
}

/**
 * properties for index view (user info )
 */
external interface IndexViewProps : Props {
    /**
     * Currently logged-in user or null
     */
    var userInfo: UserInfo?
}
