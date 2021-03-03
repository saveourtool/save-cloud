package org.cqfn.save.frontend.components

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

class CollectionView : RComponent<RProps, RState>() {
    override fun RBuilder.render() {
        child(tableComponent(
            data = arrayOf(
                Project("cqfn", "diktat", "gh", "https://github.com/cqfn/diktat", null)
            ),
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
        )) {

        }
    }
}
