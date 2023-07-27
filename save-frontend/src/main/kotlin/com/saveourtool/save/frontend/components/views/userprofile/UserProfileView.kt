/**
 * View for UserProfile
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.userprofile

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.frontend.TabMenuBar
import com.saveourtool.save.frontend.components.basic.renderAvatar
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
import com.saveourtool.save.validation.FrontendRoutes

import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.router.dom.Link
import web.cssom.*

val userProfileView: FC<UserProfileViewProps> = FC { props ->
    useBackground(Style.SAVE_LIGHT)

    val (userName, _) = useStateFromProps(props.userName)
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
            className = ClassName("col-2 mb-4 mt-2")

            renderLeftUserMenu(user, organizations)
        }

        // ===================== RIGHT COLUMN =======================================================================
        div {
            className = ClassName("col-6 mb-4 mt-2")
            tab(selectedMenu.name, UserProfileTab.values().map { it.name }, "nav nav-tabs mt-3") { value ->
                setSelectedMenu { UserProfileTab.valueOf(value) }
            }

            when (selectedMenu) {
                UserProfileTab.VULNERABILITIES -> renderVulnerabilityTable { this.userName = userName }
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
        override val regexForUrlClassification = "/${FrontendRoutes.PROFILE}"
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
        className = ClassName("row justify-content-center")
        renderAvatar(user, "mb-4", isLinkActive = false) {
            height = 15.rem
            width = 15.rem
        }
    }

    h1 {
        className = ClassName("h3 mb-0 text-gray-900 text-center ml-2")
        +(user?.name ?: "N/A")
    }

    div {
        className = ClassName("col")
        div {
            className = ClassName("row text-xs font-weight-bold text-info justify-content-center text-uppercase mb-1")
            +"Rating"
        }
        div {
            className = ClassName("row h5 font-weight-bold justify-content-center text-gray-800 mt-1")
            +user?.rating.toString()
        }
    }

    user?.company?.let { company ->
        div {
            className = ClassName("ml-2 mb-2 mt-2")
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
        className = ClassName("separator")
        style = jso {
            borderBottom = "0.07rem #000000".unsafeCast<BorderBottom>()
        }
        +"Organizations"
    }

    div {
        className = ClassName("latest-photos mt-3")
        div {
            className = ClassName("row")
            organizations.forEach { organization ->
                renderAvatar(organization) {
                    width = 4.rem
                    height = 4.rem
                }
            }
        }
    }
}
