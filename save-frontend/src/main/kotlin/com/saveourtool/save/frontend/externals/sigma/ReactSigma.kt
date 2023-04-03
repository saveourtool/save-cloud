@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:JsModule("@react-sigma/core")
@file:JsNonModule

package com.saveourtool.save.frontend.externals.sigma

import react.*

/**
 * External declaration of [sigmaContainer] react [FC]
 */
@JsName("SigmaContainer")
external val sigmaContainer: FC<SigmaContainerProps>

/**
 * [PropsWithChildren] for [sigmaContainer]
 */
external interface SigmaContainerProps : PropsWithChildren {
    /**
     * HTML classes that should be applied to [sigmaContainer]
     */
    var className: String?

    /**
     * CSS styles that should be applied to [sigmaContainer]
     */
    var style: dynamic

    /**
     * Component settings
     */
    var settings: dynamic

    /**
     * Graphology graph
     */
    var graph: dynamic
}

/**
 * @return graph setter
 */
@JsName("useLoadGraph")
external fun useLoadGraph(): (graph: dynamic) -> Unit

/**
 * Hook to configure the graph
 *
 * @return callback that receives json with graph settings
 */
@JsName("useSetSettings")
external fun useSetSettings(): (dynamic) -> Unit

/**
 * Hook to configure event handlers
 *
 * @return callback that receives the json with event handlers
 */
@JsName("useRegisterEvents")
external fun useRegisterEvents(): (dynamic) -> Unit

/**
 * Hook to get Sigma object, required to get current graph for example
 *
 * @return sigma object
 */
@JsName("useSigma")
external fun useSigma(): dynamic
