@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:JsModule("@react-sigma/layout-random")
@file:JsNonModule

package com.saveourtool.save.frontend.externals.sigma.layouts

import react.*

/**
 * @param settings
 * @return [LayoutInstance] with positions and assign functions
 */
@JsName("useLayoutRandom")
external fun useLayoutRandom(settings: dynamic = definedExternally): LayoutInstance
