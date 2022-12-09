/**
 * File with component that is used to load the graph into sigma
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.cpg

import com.saveourtool.save.demo.cpg.CpgGraph
import com.saveourtool.save.frontend.externals.sigma.layouts.useLayoutCircular
import com.saveourtool.save.frontend.externals.sigma.layouts.useLayoutForceAtlas2
import com.saveourtool.save.frontend.externals.sigma.paintNodes
import com.saveourtool.save.frontend.externals.sigma.toJson
import com.saveourtool.save.frontend.externals.sigma.useLoadGraph
import js.core.jso
import react.FC
import react.Props
import react.useEffect

/**
 * Component that is used to load the graph into sigma
 */
@Suppress("MAGIC_NUMBER", "COMPLEX_EXPRESSION")
val graphLoader: FC<GraphLoaderProps> = FC { props ->
    val loadGraph = useLoadGraph()
    val (_, circularAssign) = useLayoutCircular()
    val (_, atlasAssign) = useLayoutForceAtlas2(jso {
        iterations = 150
        settings = jso {
            gravity = 10
            barnesHutOptimize = true
        }
    })

    useEffect(props.cpgGraph.nodes) {
        loadGraph(props.cpgGraph.removeMultiEdges().paintNodes().toJson())
        circularAssign()
        atlasAssign()
    }
}

/**
 * [Props] for [graphLoader] functional component
 */
external interface GraphLoaderProps : Props {
    /**
     * The graph to be processed and displayed using sigma
     */
    var cpgGraph: CpgGraph
}
