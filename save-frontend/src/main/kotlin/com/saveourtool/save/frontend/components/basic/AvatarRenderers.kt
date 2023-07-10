/**
 * File containing functions to render avatars
 */

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.info.UserInfo
import js.core.jso
import react.CSSProperties
import react.ChildrenBuilder
import react.dom.html.ReactHTML.img
import react.router.dom.Link
import web.cssom.ClassName

/**
 * Placeholder for organization avatar
 */
const val ORGANIZATION_AVATAR_PLACEHOLDER = "img/company.svg"

/**
 * Placeholder for user avatar
 */
const val USER_AVATAR_PLACEHOLDER = "img/undraw_profile.svg"

/**
 * Render organization avatar or placeholder
 *
 * @param organizationDto organization to render avatar
 * @param classes classes applied to [img] html tag
 * @param link link to redirect to if clicked
 * @param styleBuilder [CSSProperties] builder
 */
fun ChildrenBuilder.renderAvatar(
    organizationDto: OrganizationDto,
    classes: String = "",
    link: String? = null,
    styleBuilder: CSSProperties.() -> Unit = {},
) = renderAvatar(organizationDto.avatar ?: ORGANIZATION_AVATAR_PLACEHOLDER, classes, link, styleBuilder)

/**
 * Render user avatar or placeholder
 *
 * @param userInfo user to render avatar
 * @param classes classes applied to [img] html tag
 * @param link link to redirect to if clicked
 * @param styleBuilder [CSSProperties] builder
 */
fun ChildrenBuilder.renderAvatar(
    userInfo: UserInfo,
    classes: String = "",
    link: String? = null,
    styleBuilder: CSSProperties.() -> Unit = {},
) = renderAvatar(userInfo.avatar ?: USER_AVATAR_PLACEHOLDER, classes, link, styleBuilder)

private fun ChildrenBuilder.renderAvatar(
    avatarLink: String,
    classes: String,
    link: String?,
    styleBuilder: CSSProperties.() -> Unit,
) {
    val renderImg: ChildrenBuilder.() -> Unit = {
        img {
            className = ClassName("avatar avatar-user border color-bg-default rounded-circle $classes")
            src = avatarLink
            style = jso { styleBuilder() }
        }
    }
    link?.let {
        Link {
            to = it
            renderImg()
        }
    } ?: renderImg()
}
