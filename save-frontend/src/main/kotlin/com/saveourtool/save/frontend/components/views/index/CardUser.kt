/**
 * On Index view we should have a small personal feed for the user with all useful links. It will be implemented here.
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.frontend.components.basic.renderAvatar
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.validation.FrontendRoutes

import io.ktor.util.*
import js.core.jso
import react.ChildrenBuilder
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

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

// FixMe: Some statistics: may be how many users used your demo or how many contests you created,
// FixMe: Statistics about demo

private const val REGISTER_NOW = """
    For the better User Experience we recommend you to register or sign into the SaveOurTool platform 
    using one of the supported Oauth Providers. Anyway, you can proceed without registration, 
    but functionality will be limited.
"""

private const val START_NOW = """
    The easiest way to start working with our Ecosystem is to create your organization, invite collaborators and 
    start working with services that you like. 
"""

@Suppress(
    "PARAMETER_NAME_IN_OUTER_LAMBDA",
    "LONG_LINE",
)
val cardUser: FC<IndexViewProps> = FC { props ->
    val (organizations, setOrganizations) = useState(emptyList<OrganizationDto>())
    val (countVulnerability, setCountVulnerability) = useState(0)
    val navigate = useNavigate()

    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    useRequest {
        props.userInfo?.name?.let {
            val organizationsNew: List<OrganizationDto> = get(
                "$apiUrl/organizations/get/list-by-user-name?userName=$it",
                jsonHeaders,
                loadingHandler = ::loadingHandler,
            )
                .decodeFromJsonString()

            setOrganizations(organizationsNew)

            val countVuln: Int = get(
                "$apiUrl/vulnerabilities/count-by-user?userName=$it",
                jsonHeaders,
                loadingHandler = ::loadingHandler,
            )
                .decodeFromJsonString()

            setCountVulnerability(countVuln)
        }
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
                        +"Registered since: ${it.createDate?.toInstant(TimeZone.UTC)?.toLocalDateTime(TimeZone.UTC)
                            .let { date ->
                                "${date?.dayOfMonth} ${date?.month?.name?.toLowerCasePreservingASCIIRules()} ${date?.year}"
                            }} !"
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

        props.userInfo?.let {
            div {
                className = ClassName("mt-2")
                div {
                    className = ClassName("row d-flex justify-content-center text-gray-900 mt-2")
                    h5 {
                        style = jso {
                            textAlign = TextAlign.center
                        }
                        +"Your organizations:"
                    }
                }
                if (organizations.isEmpty()) {
                    div {
                        className = ClassName("row d-flex justify-content-center mt-1")
                        buttonBuilder(
                            "Create",
                            style = "primary rounded-pill",
                            isOutline = false
                        ) {
                            navigate(to = "/${FrontendRoutes.CREATE_ORGANIZATION}")
                        }
                    }
                } else {
                    organizations.forEach { organization ->
                        div {
                            className = ClassName("row d-flex justify-content-center")
                            div {
                                className = ClassName("col-12 mt-2")
                                val renderImg: ChildrenBuilder.() -> Unit = {
                                    renderAvatar(organization) {
                                        height = 2.rem
                                        width = 2.rem
                                    }
                                    +" ${organization.name}"
                                }
                                if (organization.status != OrganizationStatus.DELETED) {
                                    Link {
                                        to = "/${organization.name}"
                                        renderImg()
                                    }
                                } else {
                                    renderImg()
                                }
                            }
                        }
                    }
                }
            }

            div {
                className = ClassName("mt-2")
                div {
                    className = ClassName("row d-flex justify-content-center text-gray-900 mt-2")
                    h5 {
                        style = jso {
                            textAlign = TextAlign.center
                        }
                        +"Your statistics:"
                    }
                }
                div {
                    className = ClassName("row text-muted border-bottom border-gray mx-3")
                    div {
                        className = ClassName("col-9")
                        p {
                            +"Vulnerabilities: "
                        }
                    }

                    div {
                        className = ClassName("col-3")
                        p {
                            Link {
                                to = "/${FrontendRoutes.PROFILE}/${props.userInfo?.name}"
                                +countVulnerability.toString()
                            }
                        }
                    }
                }

                div {
                    className = ClassName("row text-muted border-bottom border-gray mx-3")
                    div {
                        className = ClassName("col-9")
                        p {
                            +"Top rating: "
                        }
                    }

                    div {
                        className = ClassName("col-3")
                        p {
                            Link {
                                to = "/${FrontendRoutes.VULN_TOP_RATING}"
                                +"${props.userInfo?.rating ?: 0}"
                            }
                        }
                    }
                }
            }
        }
    }
}
