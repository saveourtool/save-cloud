package org.cqfn.save.frontend.components.views

import kotlinx.browser.document
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.unsafeMap

import org.w3c.fetch.Headers
import react.PropsWithChildren
import react.RBuilder
import react.RComponent
import react.State
import react.buildElement
import react.dom.a
import react.dom.button
import react.dom.div
import react.dom.td
import react.table.columns

import kotlinx.browser.window
import kotlinx.html.ButtonType

/**
 * A view with collection of projects
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class CollectionView : RComponent<PropsWithChildren, State>() {
    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR", "TOO_LONG_FUNCTION", "MAGIC_NUMBER")
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
                            a(href = "#/${it.value.owner}/${it.value.name}") {
                                +it.value.name
                            }
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
                url = "${window.location.origin}/projects/not-deleted",
                headers = Headers().also {
                    it.set("Accept", "application/json")
                },
            )
                .unsafeMap {
                    it.decodeFromJsonString<Array<Project>>()
                }
        }) { }
    }

    // A small hack to avoid duplication of main content-wrapper from App.kt
    // We will change the background only for sign-up and sign-in views
    override fun componentDidMount() {
        document.getElementById("content-wrapper")?.setAttribute(
            "style",
            "background: bg-light"
        )

        val topBar = document.getElementById("navigation-top-bar")
        topBar?.setAttribute(
            "class",
            "navbar navbar-expand bg-dark navbar-dark topbar mb-3 static-top shadow mr-1 ml-1 rounded"
        )

        topBar?.setAttribute(
            "style",
            "background: bg-dark"
        )
    }
}
