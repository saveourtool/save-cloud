@file:JsModule("react-tsparticles")
@file:JsNonModule


package com.saveourtool.save.frontend.externals.animations

import react.*

@JsName("default")
external class Particles : Component<ParticlesProps, State> {
    override fun render(): ReactElement<ParticlesProps>?
}

@JsName("IParticlesProps")
external interface ParticlesProps : PropsWithChildren {
    var id: String
    var url: String
}
