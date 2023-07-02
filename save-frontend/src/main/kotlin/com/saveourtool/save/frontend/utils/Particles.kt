package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.externals.animations.Particles
import react.ChildrenBuilder
import react.VFC
import react.react

fun ChildrenBuilder.particles(enabled: Boolean = true) {
    if (enabled) {
        // FixMe: Note that they block user interactions. Particles are superimposed on top of the view in some transitions
        // https://github.com/matteobruni/tsparticles/discussions/4489
        Particles::class.react {
            id = "tsparticles"
            url = "${kotlinx.browser.window.location.origin}/particles.json"
        }
    }
}
