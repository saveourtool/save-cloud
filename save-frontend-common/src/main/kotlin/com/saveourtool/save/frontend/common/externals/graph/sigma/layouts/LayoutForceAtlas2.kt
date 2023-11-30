@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:JsModule("@react-sigma/layout-forceatlas2")
@file:JsNonModule

package com.saveourtool.save.frontend.common.externals.graph.sigma.layouts

import react.*

/**
 * @param settings
 * @return [LayoutInstance] with positions and assign functions
 */
@JsName("useLayoutForceAtlas2")
external fun useLayoutForceAtlas2(settings: dynamic = definedExternally): LayoutInstance
