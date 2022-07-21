@file:JsModule("react-tsparticles")
@file:JsNonModule


package com.saveourtool.save.frontend.externals.animations

import react.*

@JsName("Particles")
external class Particles : Component<ParticlesProps, State> {
    override fun render(): ReactElement<ParticlesProps>?
}

@JsName("IParticlesProps")
external interface ParticlesProps : PropsWithChildren {
    var id: String
    var width: String
    var height: String
    var url: String
}
