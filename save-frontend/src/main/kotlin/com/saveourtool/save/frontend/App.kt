/**
 * Main entrypoint for SAVE frontend
 */

package com.saveourtool.save.frontend

import com.saveourtool.save.frontend.components.*
import com.saveourtool.save.frontend.components.basic.cookieBanner
import com.saveourtool.save.frontend.components.basic.scrollToTopButton
import com.saveourtool.save.frontend.components.topbar.topBarComponent
import com.saveourtool.save.frontend.externals.i18next.initI18n
import com.saveourtool.save.frontend.externals.modal.ReactModal
import com.saveourtool.save.frontend.routing.basicRouting
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso

import react.*
import react.dom.client.createRoot
import react.dom.html.ReactHTML.div
import web.cssom.ClassName
import web.dom.document
import web.html.HTMLElement

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import react.router.dom.RouterProvider
import react.router.dom.createBrowserRouter

/**
 * Main component for the whole App
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress("VARIABLE_NAME_INCORRECT_FORMAT", "NULLABLE_PROPERTY_TYPE", "EMPTY_BLOCK_STRUCTURE_ERROR")
val App = FC {
    val (userInfo, setUserInfo) = useState<UserInfo?>(null)
    useRequest {
        get(
            "$apiUrl/users/user-info",
            jsonHeaders,
            loadingHandler = ::loadingHandler,
        ).run {
            val responseText = text().await()
            if (ok && responseText.isNotEmpty() && responseText != "null") {
                val userInfoNew: UserInfo = Json.decodeFromString(responseText)
                setUserInfo(userInfoNew)
            }
        }
    }

    val index = FC {
        requestModalHandler {
            this.userInfo = userInfo
            div {
                className = ClassName("d-flex flex-column")
                id = "content-wrapper"
                ErrorBoundary::class.react {
                    topBarComponent { this.userInfo = userInfo }
                    div {
                        className = ClassName("container-fluid")
                        id = "common-save-container"
                        basicRouting {
                            this.userInfo = userInfo
                            this.userInfoSetter = setUserInfo
                        }
                    }
                    if (kotlinx.browser.window.location.pathname != "/${FrontendRoutes.COOKIE}") {
                        cookieBanner { }
                    }
                    footer { }
                }
            }
        }
        scrollToTopButton()
    }
    RouterProvider {
        router = createBrowserRouter(
            routes = arrayOf(
                jso {
                    path = "/"
                    element = index.create()
                }
            )
        )
    }
}

fun main() {
    /* Workaround for issue: https://youtrack.jetbrains.com/issue/KT-31888 */
    @Suppress("UnsafeCastFromDynamic")
    if (window.asDynamic().__karma__) {
        return
    }

    kotlinext.js.require("../scss/save-frontend.scss")  // this is needed for webpack to include resource
    kotlinext.js.require("bootstrap")  // this is needed for webpack to include bootstrap
    ReactModal.setAppElement(document.getElementById("wrapper") as HTMLElement)  // required for accessibility in react-modal

    initI18n()
    val mainDiv = document.getElementById("wrapper") as HTMLElement
    createRoot(mainDiv).render(App.create())
}
