package org.cqfn.save.frontend.components

import generated.SAVE_VERSION
import kotlinx.js.jso
import org.cqfn.save.frontend.externals.render
import org.cqfn.save.frontend.externals.screen
import org.cqfn.save.info.UserInfo
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSpanElement
import react.*
import kotlin.js.Promise
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull

class TopBarTest {
    /**
     * Ignored because `useLocation()` hook requires additional preparation for tests
     */
    @Test
    @Ignore
    fun top_bar_should_render_with_user_info() {
        render(
            createElement(type = topBar(), props = jso {
                userInfo = UserInfo("Test User")
            }),
            undefined
        )

        val userInfoSpan = screen.queryByText<HTMLSpanElement?>("Test User")
        assertNotNull(userInfoSpan)
    }
}
