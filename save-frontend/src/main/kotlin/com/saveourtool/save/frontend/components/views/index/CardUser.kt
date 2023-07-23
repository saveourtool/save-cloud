package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.components.topbar.logoSize
import com.saveourtool.save.frontend.utils.AVATAR_PLACEHOLDER
import com.saveourtool.save.frontend.utils.AVATAR_PROFILE_PLACEHOLDER
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.frontend.utils.useStateFromProps
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.FC
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import react.router.Navigate
import web.cssom.ClassName
import web.cssom.TextAlign

// FixMe: List of organizations where user included, if not - link to creation of organization
// FixMe: Current Rating in Vulnerabilities
// FixMe: Link to settings where user can install avatars
// FixMe: Latest notifications - for example: your Vuln was accepted or Change requested
// FixMe: Some statistics: may be how many users used your demo or how many contests you created,
// FixMe: How many vuln were submitted and accepted, ranking in TOP ratings: for contests and more
// FixMe: Statistics about demo
// FixMe: Registration date

private const val REGISTER_NOW = """
    For the better User Experience we recommend you to register or sign into the SaveOurTool platform 
    using one of the supported Oauth Providers. Anyway, you can proceed without registration, 
    but functionality will be limited.
"""

private const val START_NOW = """
    The easiest way to start working with our Ecosystem is to create your organization, invite collaborators and 
    start working with services that you like. 
"""

val cardUser = FC<IndexViewProps> { props ->
    val (avatar, setAvatar) = useStateFromProps("/api/$v1/avatar${props.userInfo?.avatar}")

    div {
        className = ClassName("col-3 mx-2 mt-2")
        div {
            className = ClassName("row d-flex justify-content-center")
            cardImage("img/icon1.png")
        }

        div {
            className = ClassName("row d-flex justify-content-center text-gray-900 mt-2")
            h5 {
                style = jso {
                    textAlign = TextAlign.center
                }
                +"Welcome${props.userInfo?.name?.let { ", " } ?: ""}"
                b {
                    +(props.userInfo?.name?.let { " @$it " } ?: "")
                }
                +"!"
            }
        }

        div {
            className = ClassName("row d-flex justify-content-center")
            div {
                className = ClassName("col-3")
                img {
                    className =
                        ClassName("ml-2 align-self-center avatar avatar-user width-full border color-bg-default rounded-circle fas mr-2")
                    src = props.userInfo?.avatar?.let { avatar } ?: AVATAR_PROFILE_PLACEHOLDER
                    style = logoSize
                    onError = {
                        setAvatar { AVATAR_PLACEHOLDER }
                    }
                }
            }

            div {
                className = ClassName("col-9")
                props.userInfo?.let {
                    p {
                        +START_NOW
                    }

                    div {
                        className = ClassName("row d-flex justify-content-center mt-1")
                        buttonBuilder(
                            "Profile Settings",
                            style = "primary rounded-pill",
                            isOutline = false
                        ) {
                            Navigate {
                                to = "/${FrontendRoutes.SETTINGS_PROFILE}"
                            }
                        }
                    }

                }
                    ?: run {
                        p {
                            +REGISTER_NOW
                        }

                        div {
                            className = ClassName("row d-flex justify-content-center")

                            buttonBuilder(
                                "Vulnerabilities Archive",
                                style = "primary rounded-pill",
                                isOutline = false
                            ) {
                                Navigate {
                                    to = "/${FrontendRoutes.VULNERABILITIES}"
                                }
                            }
                        }

                        div {
                            className = ClassName("row d-flex justify-content-center mt-1")
                            buttonBuilder(
                                "Save Cloud Platform",
                                style = "primary rounded-pill",
                                isOutline = false
                            ) {
                                Navigate {
                                    to = "/${FrontendRoutes.PROJECTS}"
                                }
                            }
                        }
                    }
            }
        }
    }
}
