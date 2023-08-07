/**
 * On Index view we should have a small personal feed for the user with all useful links. It will be implemented here.
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.frontend.components.basic.renderAvatar
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.FC
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.p
import react.router.dom.Link
import react.router.useNavigate
import react.useState
import web.cssom.ClassName
import web.cssom.TextAlign
import web.cssom.rem

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

val cardUser: FC<IndexViewProps> = FC { props ->
    val (organizations, setOrganizations) = useState(emptyList<OrganizationDto>())
    val navigate = useNavigate()

    useRequest {
        val organizationsNew: List<OrganizationDto> = get(
            "$apiUrl/organizations/get/list-by-user-name?userName=${props.userInfo?.name}",
            jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()

        setOrganizations(organizationsNew)
    }

    div {
        className = ClassName("col-3 mx-2 mt-2")
        div {
            className = ClassName("row d-flex justify-content-center")
            cardImage("/img/icon1.png")
        }

        div {
            className = ClassName("row d-flex justify-content-center text-gray-900 mt-2")
            h5 {
                style = jso {
                    textAlign = TextAlign.center
                }
                +"Welcome${props.userInfo?.name?.let { ", " } ?: ""}"
                Link {
                    to = "/${FrontendRoutes.PROFILE}/${props.userInfo?.name}"
                    b {
                        +(props.userInfo?.name?.let { " @$it " } ?: "")
                    }
                }
                +"!"
            }
        }

        div {
            className = ClassName("row d-flex justify-content-center")
            @Suppress("MAGIC_NUMBER")
            div {
                className = ClassName("col-3")
                renderAvatar(
                    props.userInfo,
                    "align-self-center avatar avatar-user width-full border color-bg-default rounded-circle fas mx-2"
                ) {
                    height = 4.rem
                    width = 4.rem
                }
            }

            div {
                className = ClassName("col-9")
                props.userInfo?.let {
                    p {
                        +"Registered since ${it.createDate} !"
                    }
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
                            navigate(to = "/${FrontendRoutes.SETTINGS_PROFILE}")
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
                                navigate(to = "/${FrontendRoutes.VULNERABILITIES}")
                            }
                        }

                        div {
                            className = ClassName("row d-flex justify-content-center mt-1")
                            buttonBuilder(
                                "Save Cloud Platform",
                                style = "primary rounded-pill",
                                isOutline = false
                            ) {
                                navigate(to = "/${FrontendRoutes.PROJECTS}")
                            }
                        }
                    }
            }
        }
    }
}
