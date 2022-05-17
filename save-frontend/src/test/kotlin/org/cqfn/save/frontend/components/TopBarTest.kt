package org.cqfn.save.frontend.components

import generated.SAVE_VERSION
import history.InitialEntry
import kotlinx.browser.document
import kotlinx.js.jso
import org.cqfn.save.frontend.externals.render
import org.cqfn.save.frontend.externals.screen
import org.cqfn.save.frontend.externals.userEvent
import org.cqfn.save.info.UserInfo
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSpanElement
import react.*
import react.router.MemoryRouter
import kotlin.js.Promise
import kotlin.test.*

/**
 * [MemoryRouter] is used to enable usage of `useLocation` hook inside the component
 * todo: functionality that is not covered
 * * How breadcrumbs are displayed
 * * `/execution` is trimmed from breadcrumbs
 * * global role is displayed when present
 */
class TopBarTest {
    @Test
    fun top_bar_should_render_with_user_info() {
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

        val userInfoSpan = screen.queryByText<HTMLSpanElement?>("Test User")
        assertNotNull(userInfoSpan)

        // push the button
        userEvent.click(rr.container.querySelector("[id=\"userDropdown\"]"))
        val dropdown = rr.container.querySelector("[aria-labelledby=\"userDropdown\"]") as HTMLDivElement
        assertEquals(3, dropdown.children.length)
    }

    @Test
    fun top_bar_should_render_without_user_info() {
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

        val userInfoSpan = screen.queryByText<HTMLSpanElement?>("Test User")
        assertNull(userInfoSpan)

        // push the button
        userEvent.click(rr.container.querySelector("[id=\"userDropdown\"]"))
        val dropdown = rr.container.querySelector("[aria-labelledby=\"userDropdown\"]") as HTMLDivElement
        assertEquals(1, dropdown.children.length)
    }
}
