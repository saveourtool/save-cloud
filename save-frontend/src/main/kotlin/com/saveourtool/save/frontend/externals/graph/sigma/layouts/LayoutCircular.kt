@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.externals.graph.sigma.layouts

/**
 * @param settings
 * @return [LayoutInstance] with positions and assign functions
 */
@JsModule("@react-sigma/layout-circular")
@JsNonModule
@JsExport
external fun useLayoutCircular(settings: dynamic = definedExternally): LayoutInstance
