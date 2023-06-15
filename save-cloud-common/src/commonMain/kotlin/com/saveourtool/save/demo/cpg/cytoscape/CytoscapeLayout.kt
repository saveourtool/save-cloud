package com.saveourtool.save.demo.cpg.cytoscape

/**
 * @property layoutName lib-defined name of layout
 */
enum class CytoscapeLayout(val layoutName: String) {
    BREADTHFIRST("breadthfirst"),
    CIRCLE("circle"),
    CONCENTRIC("concentric"),
    COSE("cose"),
    GRID("grid"),
    NULL("null"),
    PRESET("preset"),
    RANDOM("random"),
    ;
}
