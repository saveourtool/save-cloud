package org.cqfn.save.frontend.components.views

import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.components.tables.tableComponent
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.child
import react.dom.a
import react.dom.td
import react.table.columns

/**
 * A view with collection of projects
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
class CollectionView : RComponent<RProps, RState>() {
    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
    override fun RBuilder.render() {
        child(tableComponent(
            columns = columns {
                column(id = "index", header = "#") {
                    td {
                        +"${it.row.index}"
                    }
                }
                column(id = "name", header = "Name") {
                    td {
                        a(href = "#/${it.value.type}/${it.value.owner}/${it.value.name}") {
                            +it.value.name
                        }
                    }
                }
                column(id = "passed", header = "Tests passed") {
                    td {
                        a(href = "#/${it.value.type}/${it.value.owner}/${it.value.name}/history") {
                            +"over 9000"
                        }
                    }
                }
            }
        ) {
            arrayOf(
                Project("cqfn", "diktat", "gh", "https://github.com/cqfn/diktat", null),
            )
        }) { }
    }
}
