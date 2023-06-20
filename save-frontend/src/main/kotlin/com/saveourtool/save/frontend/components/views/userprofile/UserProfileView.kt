/**
 * View for UserProfile
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.userprofile

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.frontend.TabMenuBar
import com.saveourtool.save.frontend.components.views.contests.tab
import com.saveourtool.save.frontend.externals.fontawesome.faCity
import com.saveourtool.save.frontend.externals.fontawesome.faEnvelope
import com.saveourtool.save.frontend.externals.fontawesome.faGithub
import com.saveourtool.save.frontend.externals.fontawesome.faGlobe
import com.saveourtool.save.frontend.externals.fontawesome.faLink
import com.saveourtool.save.frontend.externals.fontawesome.faTwitter
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes

import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.figure
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.router.dom.Link
import web.cssom.*

val userProfileView: FC<UserProfileViewProps> = FC { props ->
    useBackground(Style.WHITE)

    val (userName, setUserName) = useStateFromProps(props.userName)
    val (user, setUser) = useState<UserInfo?>(null)
    val (organizations, setOrganizations) = useState<List<OrganizationDto>>(emptyList())
    val (selectedMenu, setSelectedMenu) = useState(UserProfileTab.VULNERABILITIES)

    useRequest {
        val userNew: UserInfo = get(
            "$apiUrl/users/$userName",
            Headers().apply {
                set("Accept", "application/json")
            },
            loadingHandler = ::noopLoadingHandler,
        )
            .decodeFromJsonString()

        setUser(userNew)

        val organizationsNew: List<OrganizationDto> = get(
            "$apiUrl/organizations/get/list-by-user-name?userName=$userName",
            Headers().apply {
                set("Accept", "application/json")
            },
            loadingHandler = ::noopLoadingHandler,
        )
            .decodeFromJsonString()

        setOrganizations(organizationsNew)
    }

    div {
        className = ClassName("row justify-content-center")

        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-2")

            renderLeftUserMenu(user, organizations)
        }

        // ===================== RIGHT COLUMN =======================================================================
        div {
            className = ClassName("col-6")

            div {
                className = ClassName("mb-4 mt-2")
                tab(selectedMenu.name, UserProfileTab.values().map { it.name }, "nav nav-tabs mt-3") { value ->
                    setSelectedMenu { UserProfileTab.valueOf(value) }
                }

                when (selectedMenu) {
                    UserProfileTab.VULNERABILITIES -> renderVulnerabilityTable { this.userName = userName }
                }
            }
        }
    }
}

/**
 * [Props] of user profile view component
 */
@Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "TYPE_ALIAS",
)
external interface UserProfileViewProps : Props {
    /**
     * User name
     */
    var userName: String
}

/**
 * Enum that contains values for vulnerability
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class UserProfileTab {
    VULNERABILITIES,
    ;

    companion object : TabMenuBar<UserProfileTab> {
        override val nameOfTheHeadUrlSection = ""
        override val defaultTab: UserProfileTab = VULNERABILITIES
        override val regexForUrlClassification = "/${FrontendRoutes.PROFILE.path}"
        override fun valueOf(elem: String): UserProfileTab = UserProfileTab.valueOf(elem)
        override fun values(): Array<UserProfileTab> = UserProfileTab.values()
    }
}

/**
 * @param user
 * @param organizations
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "CyclomaticComplexMethod",
)
fun ChildrenBuilder.renderLeftUserMenu(
    user: UserInfo?,
    organizations: List<OrganizationDto>,
) {
    div {
        className = ClassName("mb-4 mt-2")
        figure {
            img {
                className = ClassName("img-fluid px-sm-3")
                style = jso {
                    borderRadius = "50%".unsafeCast<BorderRadius>()
                }
                src = user?.avatar?.let { path ->
                    "/api/$v1/avatar$path"
                }
                    ?: run {
                        "img/undraw_profile.svg"
                    }
                alt = ""
            }
        }

        h1 {
            className = ClassName("h3 mb-0 text-gray-800 text-center ml-2")
            +(user?.name ?: "N/A")
        }

        div {
            className = ClassName("text-center")
            div {
                className = ClassName("text-xs font-weight-bold text-info text-uppercase mb-1 ml-2 justify-content-center")
                style = jso {
                    display = Display.flex
                    alignItems = AlignItems.center
                    alignSelf = AlignSelf.start
                }
                +"Rating"
            }
            div {
                className = ClassName("text-center h5 mb-0 font-weight-bold text-gray-800 mt-1 ml-2")
                style = jso {
                    justifyContent = JustifyContent.center
                    display = Display.flex
                    alignItems = AlignItems.center
                    alignSelf = AlignSelf.start
                }
                +user?.rating.toString()
            }
        }

        user?.company?.let { company ->
            div {
                className = ClassName("ml-2 mb-2")
                fontAwesomeIcon(icon = faCity) {
                    it.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                }
                +company
            }
        }

        user?.location?.let { location ->
            div {
                className = ClassName("ml-2 mb-2")
                fontAwesomeIcon(icon = faGlobe) {
                    it.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                }
                +location
            }
        }

        user?.gitHub?.let { gitHub ->
            div {
                className = ClassName("ml-2 mb-2")
                fontAwesomeIcon(icon = faGithub) {
                    it.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                }
                Link {
                    to = gitHub
                    +gitHub.substringAfterLast("/")
                }
            }
        }

        user?.twitter?.let { twitter ->
            div {
                className = ClassName("ml-2 mb-2")
                fontAwesomeIcon(icon = faTwitter) {
                    it.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                }
                Link {
                    to = twitter
                    +twitter.substringAfterLast("/")
                }
            }
        }

        user?.linkedin?.let { linkedin ->
            div {
                className = ClassName("ml-2 mb-2")
                fontAwesomeIcon(icon = faLink) {
                    it.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                }
                Link {
                    to = linkedin
                    +"in/${linkedin.substringAfterLast("/")}"
                }
            }
        }

        user?.email?.let { email ->
            div {
                className = ClassName("ml-2 mb-2")
                fontAwesomeIcon(icon = faEnvelope) {
                    it.className = "fas fa-sm fa-fw mr-2 text-gray-600"
                }
                Link {
                    to = email
                    +email
                }
            }
        }

        div {
            className = ClassName("latest-photos")
            div {
                className = ClassName("row")
                organizations.forEach { organization ->
                    div {
                        className = ClassName("col-md-3")
                        figure {
                            Link {
                                img {
                                    className = ClassName("img-fluid")
                                    style = jso {
                                        borderRadius = "40%".unsafeCast<BorderRadius>()
                                    }
                                    src = organization.avatar?.let { path ->
                                        "/api/$v1/avatar$path"
                                    }
                                        ?: run {
                                            "img/undraw_profile.svg"
                                        }
                                    alt = ""
                                }
                                to = "/${organization.name}"
                            }
                        }
                    }
                }
            }
        }
    }
}
