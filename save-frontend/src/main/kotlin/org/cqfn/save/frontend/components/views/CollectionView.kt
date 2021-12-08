@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport")

package org.cqfn.save.frontend.components.views

import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.components.basic.privacySpan
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.unsafeMap

import org.w3c.fetch.Headers
import react.*
import react.dom.*
import react.table.columns

import kotlinx.html.ButtonType

/**
 * A view with collection of projects
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class CollectionView : AbstractView<PropsWithChildren, State>(false) {
    @Suppress(
        "EMPTY_BLOCK_STRUCTURE_ERROR",
        "TOO_LONG_FUNCTION",
        "MAGIC_NUMBER",
        "LongMethod")
    override fun RBuilder.render() {
        div {
            button(type = ButtonType.button, classes = "btn btn-primary mb-2") {
                a(classes = "text-light", href = "#/creation/") {
                    +"Add new tested tool"
                }
            }
        }
        child(tableComponent(
            columns = columns {
                column(id = "owner", header = "Project Owner") {
                    buildElement {
                        td {
                            a(href = "#/${it.value.owner}") {
                                +it.value.owner
                            }
                        }
                    }
                }
                column(id = "name", header = "Evaluated Tool") {
                    buildElement {
                        td {
                            a(href = "#/${it.value.owner}/${it.value.name}") { +it.value.name }
                            privacySpan(it.value)
                        }
                    }
                }
                column(id = "passed", header = "Description") {
                    buildElement {
                        td {
                            +(it.value.description ?: "Description not provided")
                        }
                    }
                }
                column(id = "rating", header = "Contest Rating") {
                    buildElement {
                        td {
                            +"0"
                        }
                    }
                }
            },
            initialPageSize = 10,
            useServerPaging = false,
            usePageSelection = false,
        ) { _, _ ->
            get(
                url = "$apiUrl/projects/not-deleted",
                headers = Headers().also {
                    it.set("Accept", "application/json")
                },
            )
                .unsafeMap {
                    it.decodeFromJsonString<Array<Project>>()
                }
        }) { }
    }
}
