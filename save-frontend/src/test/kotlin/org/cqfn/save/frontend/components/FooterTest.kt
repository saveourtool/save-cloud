package org.cqfn.save.frontend.components

import generated.SAVE_VERSION
import kotlinx.js.jso
import org.cqfn.save.frontend.externals.render
import org.cqfn.save.frontend.externals.screen
import org.w3c.dom.HTMLElement
import react.createElement
import react.react
import kotlin.test.Test
import kotlin.test.assertNotNull

class FooterTest {
    @Test
    fun footer_should_render() {
        render(
            createElement(type = Footer::class.react),
        )

        val versionLabel = screen.queryByText("Version $SAVE_VERSION", jso {
            // match substring
            exact = false
        }) as HTMLElement?
        assertNotNull(versionLabel)
    }
}
