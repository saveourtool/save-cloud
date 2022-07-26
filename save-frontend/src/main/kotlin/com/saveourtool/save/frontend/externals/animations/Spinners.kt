@file:JsModule("react-spinners")
@file:JsNonModule

package com.saveourtool.save.frontend.externals.animations

import react.*

@JsName("LoaderSizeProps")
external interface LoaderSizeProps : PropsWithChildren

/**
 * @param options
 * @return
 */
@JsName("RingLoader")
external fun ringLoader(options: dynamic = definedExternally): ReactElement<LoaderSizeProps>?
