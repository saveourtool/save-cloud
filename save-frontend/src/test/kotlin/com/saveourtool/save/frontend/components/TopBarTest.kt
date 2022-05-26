package com.saveourtool.save.frontend.components

import com.saveourtool.save.frontend.externals.*
import com.saveourtool.save.info.UserInfo

import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLSpanElement
import react.*
import react.router.MemoryRouter

import kotlin.test.*
import kotlinx.js.jso

/**
 * [MemoryRouter] is used to enable usage of `useLocation` hook inside the component
 * todo: functionality that is not covered
 * * How breadcrumbs are displayed
 * * `/execution` is trimmed from breadcrumbs
 * * global role is displayed when present
 */
class TopBarTest {
    @Test
    fun topBarShouldRenderWithUserInfo() {
        val rr = render(
            MemoryRouter.create {
                initialEntries = arrayOf(
                    "/"
                )
                topBar().invoke {
                    userInfo = UserInfo("Test User")
                }
            }
        )

        val userInfoSpan: HTMLSpanElement? = screen.queryByTextAndCast("Test User")
        assertNotNull(userInfoSpan)

        // push the button
        val button = screen.getByRole("button", jso { name = "Test User" })
        userEvent.click(button)
        val dropdown = rr.container.querySelector("[aria-labelledby=\"userDropdown\"]") as HTMLDivElement
        assertEquals(3, dropdown.children.length, "When user is logged in, dropdown menu should contain 3 entries")
    }

    @Test
    fun topBarShouldRenderWithoutUserInfo() {
        val rr = render(
            MemoryRouter.create {
                initialEntries = arrayOf(
                    "/"
                )
                topBar().invoke {
                    userInfo = null
                }
            }
        )

        val userInfoSpan: HTMLSpanElement? = screen.queryByTextAndCast("Test User")
        assertNull(userInfoSpan)

        // push the button
        userEvent.click(rr.container.querySelector("[id=\"userDropdown\"]"))
        val dropdown = rr.container.querySelector("[aria-labelledby=\"userDropdown\"]") as HTMLDivElement
        assertEquals(1, dropdown.children.length, "When user is not logged in, dropdown menu should contain 1 entry")
    }
}
