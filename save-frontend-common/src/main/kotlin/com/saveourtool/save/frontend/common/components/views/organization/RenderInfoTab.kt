@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
)

package com.saveourtool.save.frontend.common.components.views.organization

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.entities.ProjectDto
import com.saveourtool.save.frontend.common.components.basic.scoreCard
import com.saveourtool.save.frontend.common.components.basic.userBoard
import com.saveourtool.save.frontend.common.externals.fontawesome.faCheck
import com.saveourtool.save.frontend.common.externals.fontawesome.faEdit
import com.saveourtool.save.frontend.common.externals.fontawesome.faTimesCircle
import com.saveourtool.save.frontend.common.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.common.utils.*
import com.saveourtool.save.info.UserInfo

import js.core.jso
import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.textarea
import web.cssom.AlignItems
import web.cssom.ClassName
import web.cssom.Display
import web.cssom.rem
import web.html.ButtonType

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val TOP_PROJECTS_NUMBER = 4

internal val renderInfoTab: FC<RenderInfoTabProps> = FC { props ->

    val (draftOrganizationDescription, setDraftOrganizationDescription) = useStateFromProps(props.organization.description)
    val (isEditDisabled, setIsEditDisabled) = useState(true)

    val fetchOrganizationSave = useDeferredRequest {
        props.organization.copy(
            description = draftOrganizationDescription
        ).let { organizationWithNewDescription ->
            val response = post(
                "$apiUrl/organizations/${organizationWithNewDescription.name}/update",
                jsonHeaders,
                Json.encodeToString(organizationWithNewDescription),
                loadingHandler = ::noopLoadingHandler,
            )
            if (response.ok) {
                props.setOrganization(organizationWithNewDescription)
            }
        }
    }

    // ================= Rows for TOP projects ================
    val topProjects = props.projects.sortedByDescending { it.contestRating }.take(TOP_PROJECTS_NUMBER)

    if (topProjects.isNotEmpty()) {
        // ================= Title for TOP projects ===============
        div {
            className = ClassName("row justify-content-center mb-2")
            h4 {
                +"Top Tools"
            }
        }
        div {
            className = ClassName("row justify-content-center")

            renderTopProject(topProjects.getOrNull(0), props.organization.name)
            renderTopProject(topProjects.getOrNull(1), props.organization.name)
        }

        @Suppress("MAGIC_NUMBER")
        (div {
            className = ClassName("row justify-content-center")

            renderTopProject(topProjects.getOrNull(2), props.organization.name)
            renderTopProject(topProjects.getOrNull(3), props.organization.name)
        })
    }

    div {
        className = ClassName("row justify-content-center")

        div {
            className = ClassName("col-4 mb-4")
            div {
                className = ClassName("card shadow mb-4")
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
                        if (props.selfRole.hasWritePermission() && isEditDisabled) {
                            button {
                                type = ButtonType.button
                                className = ClassName("btn btn-link text-xs text-muted text-left ml-auto")
                                +"Edit  "
                                fontAwesomeIcon(icon = faEdit)
                                onClick = {
                                    setIsEditDisabled(false)
                                }
                            }
                        }
                    }
                }
                div {
                    className = ClassName("card-body")
                    textarea {
                        className = ClassName("auto_height form-control-plaintext pt-0 pb-0")
                        value = draftOrganizationDescription
                        disabled = !props.selfRole.hasWritePermission() || isEditDisabled
                        onChange = {
                            setDraftOrganizationDescription(it.target.value)
                        }
                    }
                }
                div {
                    className = ClassName("ml-3 mt-2 align-items-right float-right")
                    button {
                        type = ButtonType.button
                        className = ClassName("btn")
                        fontAwesomeIcon(icon = faCheck)
                        hidden = !props.selfRole.hasWritePermission() || isEditDisabled
                        onClick = {
                            fetchOrganizationSave()
                            setIsEditDisabled(true)
                        }
                    }

                    button {
                        type = ButtonType.button
                        className = ClassName("btn")
                        fontAwesomeIcon(icon = faTimesCircle)
                        hidden = !props.selfRole.hasWritePermission() || isEditDisabled
                        onClick = {
                            setIsEditDisabled(true)
                        }
                    }
                }
            }
        }

        div {
            className = ClassName("col-2")
            userBoard {
                users = props.usersInOrganization
                avatarOuterClasses = "col-4 px-0"
                avatarInnerClasses = "mx-sm-3"
                widthAndHeight = 6.rem
            }
        }
    }
}

/**
 * RenderInfoTab component props
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface RenderInfoTabProps : Props {
    var usersInOrganization: List<UserInfo>
    var organization: OrganizationDto
    var setOrganization: StateSetter<OrganizationDto>
    var selfRole: Role
    var organizationName: String
    var projects: List<ProjectDto>
}

private fun ChildrenBuilder.renderTopProject(topProject: ProjectDto?, organizationName: String) {
    div {
        className = ClassName("col-3 mb-4")
        topProject?.let {
            scoreCard {
                name = it.name
                contestScore = it.contestRating
                url = "/$organizationName/${it.name}"
            }
        }
    }
}
