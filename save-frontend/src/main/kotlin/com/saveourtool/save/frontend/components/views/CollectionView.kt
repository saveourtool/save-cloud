@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.privacySpan
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.utils.apiUrl
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.frontend.utils.decodeFromJsonString
import com.saveourtool.save.frontend.utils.get
import com.saveourtool.save.frontend.utils.unsafeMap
import com.saveourtool.save.info.UserInfo

import org.w3c.fetch.Headers
import react.*
import react.dom.*
import react.table.columns

import kotlinx.html.ButtonType
import kotlinx.html.hidden

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface CreationViewProps : Props {
    var currentUserInfo: UserInfo?
}

/**
 * A view with collection of projects
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class CollectionView : AbstractView<CreationViewProps, State>(false) {
    @Suppress("MAGIC_NUMBER")
    private val projectsTable = tableComponent(
        columns = columns<Project> {
            column(id = "organization", header = "Organization", { organization.name }) { cellProps ->
                buildElement {
                    td {
                        a(href = "#/${cellProps.row.original.organization.name}") { +cellProps.value }
                    }
                }
            }
            column(id = "name", header = "Evaluated Tool", { name }) { cellProps ->
                buildElement {
                    td {
                        a(href = "#/${cellProps.row.original.organization.name}/${cellProps.value}") { +cellProps.value }
                        privacySpan(cellProps.row.original)
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
    )
    @Suppress(
        "EMPTY_BLOCK_STRUCTURE_ERROR",
        "TOO_LONG_FUNCTION",
        "MAGIC_NUMBER",
        "LongMethod",
    )
    override fun RBuilder.render() {
        div {
            attrs.hidden = (props.currentUserInfo == null)
            button(type = ButtonType.button, classes = "btn btn-primary mb-2 mr-2") {
                a(classes = "text-light", href = "#/creation/") {
                    +"Add new tested tool"
                }
            }
            button(type = ButtonType.button, classes = "btn btn-primary mb-2") {
                a(classes = "text-light", href = "#/createOrganization/") {
                    +"Add new organization"
                }
            }
        }
        child(projectsTable) {
            attrs.getData = { _, _ ->
                val response = get(
                    url = "$apiUrl/projects/not-deleted",
                    headers = Headers().also {
                        it.set("Accept", "application/json")
                    },
                    loadingHandler = ::classLoadingHandler,
                )
                if (response.ok) {
                    response.unsafeMap {
                        it.decodeFromJsonString<Array<Project>>()
                    }
                } else {
                    emptyArray()
                }
            }
        }
    }

    companion object : RStatics<CreationViewProps, State, CollectionView, Context<RequestStatusContext>>(CollectionView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
