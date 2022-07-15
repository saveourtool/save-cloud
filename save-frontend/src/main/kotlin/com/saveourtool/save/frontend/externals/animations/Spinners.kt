

package com.saveourtool.save.frontend.externals.animations

import react.*

@JsModule("react-spinners")
@JsNonModule
external class RingLoader: Component<RingLoaderProps, State> {
    override fun render(): ReactElement<RingLoaderProps>?
}

fun ChildrenBuilder.spinner() = RingLoader::class.react {
    size = 60
    color = "#000000"
    loading = true
}

external interface RingLoaderProps : PropsWithChildren {
    var size: Int
    var color: String
    var loading: Boolean
}
