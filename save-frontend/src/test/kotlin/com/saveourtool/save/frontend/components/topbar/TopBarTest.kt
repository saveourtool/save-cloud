package com.saveourtool.save.frontend.components.topbar

import com.saveourtool.save.frontend.components.basic.cookieBanner
import com.saveourtool.save.frontend.components.footer
import com.saveourtool.save.frontend.externals.*
import com.saveourtool.save.frontend.externals.i18next.initI18n
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import web.html.HTMLDivElement
import web.html.HTMLSpanElement
import react.*

import kotlin.test.*
import js.core.jso
import kotlinx.browser.window
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.router.Outlet
import react.router.createMemoryRouter
import react.router.dom.RouterProvider
import web.cssom.ClassName

/**
 * [createMemoryRouter] is used to enable usage of `useLocation` hook inside the component
 * todo: functionality that is not covered
 * * How breadcrumbs are displayed
 * * `/execution` is trimmed from breadcrumbs
 * * global role is displayed when present
 */
class TopBarTest {
    @Test
    fun topBarShouldRenderWithUserInfo() {
        val rr = render(topBarComponentView(UserInfo(name = "Test User")).create())

        val userInfoSpan: HTMLSpanElement? = screen.queryByTextAndCast("Test User")
        assertNotNull(userInfoSpan)

        // push the button, this weird test searches for the button that contains a name "Test User"
        // and "User setting" hint label
        val button = screen.getByRole("button", jso { name = "Test UserUser settings" })
        userEvent.click(button)
        val dropdown = rr.container.querySelector("[aria-labelledby=\"userDropdown\"]") as HTMLDivElement
        assertEquals(4, dropdown.children.length, "When user is logged in, dropdown menu should contain 3 entries")
    }

    @Test
    fun topBarShouldRenderWithoutUserInfo() {
        val rr = render(topBarComponentView(null).create())

        val userInfoSpan: HTMLSpanElement? = screen.queryByTextAndCast("Test User")
        assertNull(userInfoSpan)

        // push the button
        userEvent.click(rr.container.querySelector("[id=\"userDropdown\"]"))
        val dropdown = rr.container.querySelector("[aria-labelledby=\"userDropdown\"]") as HTMLDivElement
        assertEquals(1, dropdown.children.length, "When user is not logged in, dropdown menu should contain 1 entry")
    }

    companion object {
        private fun topBarComponentView(userInfo: UserInfo?) = FC {
            initI18n()
            RouterProvider {
                router = createMemoryRouter(
                    routes = arrayOf(
                        jso {
                            path = "/"
                            element = FC {
                                div {
                                    className = ClassName("d-flex flex-column")
                                    id = "content-wrapper"
                                    topBarComponent { this.userInfo = userInfo }
                                }
                            }.create()
                        }
                    ),
                    opts = jso {
                        basename = "/"
                        initialEntries = arrayOf("/")
                    }
                )
            }
        }
    }
}
