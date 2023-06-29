/**
 * View for FossGraph
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.fossgraph

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityLanguage
import com.saveourtool.save.frontend.TabMenuBar
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.components.views.contests.tab
import com.saveourtool.save.frontend.externals.fontawesome.faTrash
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import js.core.jso
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.textarea
import react.router.dom.Link
import react.router.useNavigate
import web.cssom.*

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Suppress(
    "MAGIC_NUMBER",
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "TYPE_ALIAS",
)
val fossGraph: FC<FossGraphViewProps> = FC { props ->
    useBackground(Style.WHITE)
    useTooltip()

    val deleteVulnerabilityWindowOpenness = useWindowOpenness()

    val navigate = useNavigate()

    val (vulnerability, setVulnerability) = useState(VulnerabilityDto.empty)
    val (user, setUser) = useState(props.currentUserInfo)
    val (selectedMenu, setSelectedMenu) = useState(VulnerabilityTab.INFO)

    val enrollUpdateRequest = useDeferredRequest {
        val vulnerabilityUpdate = vulnerability.copy(isActive = true)
        val response = post(
            url = "$apiUrl/vulnerabilities/approve",
            headers = jsonHeaders,
            body = Json.encodeToString(vulnerabilityUpdate),
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            navigate(to = "/${FrontendRoutes.VULNERABILITIES}")
        }
    }

    val enrollDeleteRequest = useDeferredRequest {
        val response = delete(
            url = "$apiUrl/vulnerabilities/delete?name=${props.name}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            navigate(to = "/${FrontendRoutes.VULNERABILITIES}")
        }
    }

    useRequest {
        val vulnerabilityNew: VulnerabilityDto = get(
            url = "$apiUrl/vulnerabilities/by-name-with-description?name=${props.name}",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }

        setVulnerability(vulnerabilityNew)

        val userInfo: UserInfo = get(
            url = "$apiUrl/users/${props.currentUserInfo?.name}",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .decodeFromJsonString()

        setUser(userInfo)
    }

    displayModal(
        deleteVulnerabilityWindowOpenness.isOpen(),
        "Deletion of vulnerability",
        "Are you sure you want to remove this vulnerability?",
        mediumTransparentModalStyle,
        deleteVulnerabilityWindowOpenness.closeWindowAction(),
    ) {
        buttonBuilder("Ok") {
            enrollDeleteRequest()
            deleteVulnerabilityWindowOpenness.closeWindow()
        }
        buttonBuilder("Close", "secondary") {
            deleteVulnerabilityWindowOpenness.closeWindow()
        }
    }

    div {
        className = ClassName("card card-body mt-0")

        val isSuperAdmin = props.currentUserInfo?.globalRole?.isHigherOrEqualThan(Role.SUPER_ADMIN) == true
        val isOwner = user?.id == vulnerability.userId

        div {
            className = ClassName("d-flex align-items-center justify-content-center mb-4")
            h1 {
                className = ClassName("h3 mb-0 text-center text-gray-800")
                +vulnerability.name
            }
            languageSpan(vulnerability.language)
            div {
                className = ClassName("mr-3")
                style = jso {
                    position = "absolute".unsafeCast<Position>()
                    right = "0%".unsafeCast<Left>()
                }
                if (isSuperAdmin || isOwner) {
                    buttonBuilder(
                        icon = faTrash,
                        style = "danger",
                        isOutline = true,
                        classes = "mr-2",
                        title = "Delete vulnerability",
                    ) {
                        deleteVulnerabilityWindowOpenness.openWindow()
                    }
                }
                if (isSuperAdmin && !vulnerability.isActive) {
                    buttonBuilder(label = "Approve", style = "success") {
                        enrollUpdateRequest()
                    }
                }
            }
        }

        div {
            className = ClassName("row justify-content-center")
            // ===================== LEFT COLUMN =======================================================================
            div {
                className = ClassName("col-3 mr-3")
                vulnerabilityBadge {
                    this.vulnerability = vulnerability
                }
                div {
                    className = ClassName("card shadow mb-4")

                    div {
                        className = ClassName("card-body")
                        textarea {
                            className = ClassName("auto_height form-control-plaintext pt-0 pb-0")
                            value = vulnerability.shortDescription
                            rows = 2
                            disabled = true
                        }
                    }

                    div {
                        className = ClassName("card-header py-3")
                        div {
                            className = ClassName("row")
                            h6 {
                                className = ClassName("m-0 font-weight-bold text-primary")
                                style = jso {
                                    display = Display.flex
                                    alignItems = AlignItems.center
                                }
                                +"Description"
                            }
                        }
                    }
                    div {
                        className = ClassName("card-body")
                        textarea {
                            className = ClassName("auto_height form-control-plaintext pt-0 pb-0")
                            value = "${vulnerability.description}"
                            rows = 8
                            disabled = true
                        }
                    }

                    if (!vulnerability.vulnerabilityIdentifier.isNullOrEmpty()) {
                        div {
                            className = ClassName("card-header py-3")
                            div {
                                className = ClassName("row")
                                h6 {
                                    className = ClassName("m-0 font-weight-bold text-primary d-flex align-items-center")
                                    +"Identifier"
                                }
                            }
                        }
                        div {
                            className = ClassName("card-body")
                            div {
                                +"${vulnerability.vulnerabilityIdentifier}"
                            }
                        }
                    }

                    if (!vulnerability.relatedLink.isNullOrEmpty()) {
                        div {
                            className = ClassName("card-header py-3")
                            div {
                                className = ClassName("row")
                                h6 {
                                    className = ClassName("m-0 font-weight-bold text-primary d-flex align-items-center")
                                    +"Related link"
                                }
                            }
                        }
                        div {
                            className = ClassName("card-body")
                            Link {
                                to = "${vulnerability.relatedLink}"
                                +"${vulnerability.relatedLink}"
                            }
                        }
                    }
                }
            }
            // ===================== RIGHT COLUMN =======================================================================
            div {
                className = ClassName("col-6")

                div {
                    className = ClassName("mb-4 mt-2")
                    tab(selectedMenu.name, VulnerabilityTab.values().map { it.name }, "nav nav-tabs mt-3") { value ->
                        setSelectedMenu { VulnerabilityTab.valueOf(value) }
                    }

                    when (selectedMenu) {
                        VulnerabilityTab.INFO -> vulnerabilityInfoTab {
                            this.vulnerability = vulnerability
                        }
                        VulnerabilityTab.COMMENTS -> vulnerabilityCommentTab {
                            this.vulnerability = vulnerability
                            this.currentUserInfo = user
                        }
                    }
                }
            }
        }
    }
}

/**
 * Enum that contains values for vulnerability
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class VulnerabilityTab {
    INFO,
    COMMENTS,
    ;

    companion object : TabMenuBar<VulnerabilityTab> {
        override val nameOfTheHeadUrlSection = ""
        override val defaultTab: VulnerabilityTab = INFO
        override val regexForUrlClassification = "/${FrontendRoutes.PROFILE.path}"
        override fun valueOf(elem: String): VulnerabilityTab = VulnerabilityTab.valueOf(elem)
        override fun values(): Array<VulnerabilityTab> = VulnerabilityTab.values()
    }
}

/**
 * [Props] for FossGraphView
 */
external interface FossGraphViewProps : Props {
    /**
     * Name of security vulnerabilities
     */
    var name: String

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo?
}

private fun ChildrenBuilder.languageSpan(language: VulnerabilityLanguage) {
    span {
        className = ClassName("border border-danger text-danger ml-2 pl-1 pr-1")
        style = jso {
            borderRadius = "2em".unsafeCast<BorderRadius>()
        }
        +language.value
    }
}
