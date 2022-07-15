@file:JsModule("react-spinners")
@file:JsNonModule

package com.saveourtool.save.frontend.externals.animations

import react.*

@JsName("RingLoader")
external fun ringLoader(options: dynamic = definedExternally): ReactElement<LoaderSizeProps>?

@JsName("LoaderSizeProps")
external interface LoaderSizeProps : PropsWithChildren
