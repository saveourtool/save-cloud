/**
 * A view with settings user
 */

package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.entities.OrganizationWithUsers
import com.saveourtool.save.filters.OrganizationFilter
import com.saveourtool.save.frontend.components.basic.avatarForm
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.frontend.components.views.index.*
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.http.getUser
import com.saveourtool.save.frontend.http.postImageUpload
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes

import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.events.ChangeEvent
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.nav
import react.router.dom.Link
import web.cssom.*
import web.html.HTMLInputElement

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.main

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface SettingsProps : PropsWithChildren {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?

    /**
     * just a flag for a factory
     */
    var type: FrontendRoutes
}

val userSettingsView = FC<SettingsProps> { props ->
    main {
        className = ClassName("main-content")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("row justify-content-center")
                div {
                    className = ClassName("col-2")
                    leftColumn { this.userInfo = props.userInfo }
                }
                div {
                    className = ClassName("col-7")
                    if (props.userInfo != null) {
                        rightColumn {
                            this.userInfo = props.userInfo
                            this.type = props.type
                        }
                    } else {
                        main {
                            +"404"
                        }
                    }
                }

            }
        }
    }
}

