package org.cqfn.save.frontend.components

import org.cqfn.save.frontend.externals.render
import org.cqfn.save.frontend.externals.screen

import generated.SAVE_VERSION
import org.w3c.dom.HTMLElement
import react.createElement
import react.react

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.js.jso

class FooterTest {
    @Test
    fun footerShouldRender() {
        render(
            createElement(type = Footer::class.react),
        )

        val versionLabel = screen.queryByText("Version $SAVE_VERSION", jso {
            // match substring
            exact = false
        }) as HTMLElement?
        assertNotNull(versionLabel, "Footer should contain SAVE version")
    }
}
