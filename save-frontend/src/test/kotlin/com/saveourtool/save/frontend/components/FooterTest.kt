package com.saveourtool.save.frontend.components

import com.saveourtool.save.frontend.externals.render
import com.saveourtool.save.frontend.externals.screen

import generated.SAVE_VERSION
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
        })
        assertNotNull(versionLabel, "Footer should contain SAVE version")
    }
}
