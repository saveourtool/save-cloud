package com.saveourtool.save.frontend.components

import com.saveourtool.save.frontend.externals.render
import com.saveourtool.save.frontend.externals.screen

import generated.SAVE_CLOUD_VERSION

import kotlin.test.Test
import kotlin.test.assertNotNull
import js.core.jso
import react.create

class FooterTest {
    @Test
    fun footerShouldRender() {
        render(
            footer.create()
        )

        val versionLabel = screen.queryByText("Version $SAVE_CLOUD_VERSION",
            options = jso {
                // match substring
                exact = false
            })
        assertNotNull(versionLabel, "Footer should contain SAVE version")
    }
}
