/**
 * View for UserProfile
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.userprofile

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.frontend.TabMenuBar
import com.saveourtool.save.frontend.components.basic.renderAvatar
import com.saveourtool.save.frontend.components.views.contests.tab
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import js.core.jso
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h3
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.p
import react.router.dom.Link
import react.router.useNavigate
import web.cssom.*

val userProfileView: FC<UserProfileViewProps> = FC { props ->
    useBackground(Style.SAVE_LIGHT)

    val (userName, _) = useStateFromProps(props.userName)
    val (user, setUser) = useState<UserInfo?>(null)
    val (organizations, setOrganizations) = useState<List<OrganizationDto>>(emptyList())
    val (selectedMenu, setSelectedMenu) = useState(UserProfileTab.VULNERABILITIES)
    val (vulnerabilities, setVulnerabilities) = useState<Array<VulnerabilityDto>>(emptyArray())

    useRequest {
        val userNew: UserInfo = get(
            "$apiUrl/users/$userName",
            jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()

        setUser(userNew)

        val organizationsNew: List<OrganizationDto> = get(
            "$apiUrl/organizations/get/list-by-user-name?userName=$userName",
            jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .decodeFromJsonString()

        setOrganizations(organizationsNew)

        val vulnerabilitiesNew: Array<VulnerabilityDto> = get(
            url = "$apiUrl/vulnerabilities/by-user?userName=$userName",
            jsonHeaders,
            loadingHandler = ::loadingHandler,
        ).decodeFromJsonString()

        setVulnerabilities(vulnerabilitiesNew)
    }

    div {
        className = ClassName("row justify-content-center")

        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-2 mb-4 mt-2")

            renderLeftUserMenu(user, props.currentUserInfo, organizations)
        }

        // ===================== RIGHT COLUMN =======================================================================
        div {
            className = ClassName("col-6 mb-4 mt-2")
            tab(selectedMenu.name, UserProfileTab.values().map { it.name }, "nav nav-tabs mt-3") { value ->
                setSelectedMenu { UserProfileTab.valueOf(value) }
            }

            when (selectedMenu) {
                UserProfileTab.VULNERABILITIES -> renderVulnerabilityTableForProfileView {
                    this.userName = userName
                    this.vulnerabilities = vulnerabilities
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

    /**
     * Current logged-in user
     */
    var currentUserInfo: UserInfo?
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
 * @param currentUser
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "CyclomaticComplexMethod",
)
fun ChildrenBuilder.renderLeftUserMenu(
    user: UserInfo?,
    currentUser: UserInfo?,
    organizations: List<OrganizationDto>,
) {
    val navigate = useNavigate()

    div {
        className = ClassName("row justify-content-center")
        renderAvatar(user, "mb-4", isLinkActive = false) {
            height = 15.rem
            width = 15.rem
        }
    }

    h3 {
        className = ClassName("mb-0 text-gray-900 text-center")
        +(user?.name ?: "N/A")
    }

    h5 {
        className = ClassName("mb-0 text-gray-600 text-center")
        +(user?.realName ?: "N/A")
    }

    div {
        className = ClassName("col text-center mt-2")
        Link {
            to = "/${FrontendRoutes.VULN_TOP_RATING}"
            className = ClassName("row text-xs font-weight-bold text-info justify-content-center text-uppercase mb-1")
            +"Rating"
        }
        div {
            className = ClassName("row h5 font-weight-bold justify-content-center text-gray-800 my-1")
            +user?.rating.toString()
        }

        if (currentUser?.name == user?.name) {
            div {
                className = ClassName("row h5 font-weight-bold justify-content-center text-gray-800 my-3")

                buttonBuilder(label = "Customize profile", isOutline = true, style = "primary btn-sm") {
                    navigate(to = "/${FrontendRoutes.SETTINGS_PROFILE}")
                }
            }
        }
    }

    user?.freeText?.let { freeText(it) }

    user?.company?.let { company ->
        div {
            className = ClassName("my-2")
            fontAwesomeIcon(icon = faCity) {
                it.className = "fas fa-sm fa-fw mr-2"
            }
            +company
        }
    }

    user?.location?.let { location ->
        div {
            className = ClassName("mb-2")
            fontAwesomeIcon(icon = faGlobe) {
                it.className = "fas fa-sm fa-fw mr-2"
            }
            +location
        }
    }

    user?.gitHub?.let { extraLinks(faGithub, it) }

    user?.twitter?.let { extraLinks(faTwitter, it) }

    user?.linkedin?.let { extraLinks(faLink, it) }

    user?.website?.let { extraLinks(faLink, it) }

    if (organizations.isNotEmpty()) {
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
}

/**
 * @param icon
 * @param info
 */
fun ChildrenBuilder.extraLinks(icon: FontAwesomeIconModule, info: String) {
    div {
        className = ClassName("mb-2")
        fontAwesomeIcon(icon = icon) {
            it.className = "fas fa-sm fa-fw mr-2 text-gray-900"
        }
        a {
            href = info
            +info.substringAfterLast("/")
        }
    }
}

/**
 * @param text
 */
fun ChildrenBuilder.freeText(text: String) {
    if (text.isNotEmpty()) {
        div {
            className = ClassName("separator")
            style = jso {
                borderBottom = "0.07rem #000000".unsafeCast<BorderBottom>()
            }
            +"About"
        }

        div {
            className = ClassName("row justify-content-center")
            p {
                className = ClassName("mb-0")
                style = jso {
                    textAlign = TextAlign.justify
                }
                +text
            }
        }
        @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
        hr {}
    }
}
