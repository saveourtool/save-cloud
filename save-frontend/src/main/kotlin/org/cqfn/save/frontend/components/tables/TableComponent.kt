/**
 * Utilities for react-tables
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package org.cqfn.save.frontend.components.tables

import org.cqfn.save.frontend.utils.spread

import react.RProps
import react.dom.div
import react.dom.h6
import react.dom.span
import react.dom.table
import react.dom.tbody
import react.dom.th
import react.dom.thead
import react.dom.tr
import react.functionalComponent
import react.table.Column
import react.table.TableInstance
import react.table.useSortBy
import react.table.useTable
import react.useEffect
import react.useMemo
import react.useState

import kotlin.js.json

/**
 * [RProps] of a data table
 */
external interface TableProps : RProps {
    /**
     * Table header
     */
    var tableHeader: String
}

/**
 * A [RComponent] for a data table
 *
 * @param columns columns as an array of [Column]
 * @param getData a function to retrieve data for the table, returns an array of data of type [out D] that will be inserted into the table
 * @return a functional react component
 */
@Suppress("TOO_LONG_FUNCTION", "ForbiddenComment")
fun <D : Any> tableComponent(columns: Array<out Column<D, *>>, getData: () -> Array<out D>) = functionalComponent<TableProps> { props ->
    val (data, setData) = useState<Array<out D>>(emptyArray())

    // with `emptyList` effect will be executed only once on the initial render. TODO: how to handle dynamic data updates, e.g. when paging?
    useEffect(emptyList()) {
        setData(getData())
    }

    val tableInstance: TableInstance<D> = useTable(useSortBy) {
        this.columns = useMemo { columns }
        this.data = data
    }

    div("card shadow mb-4") {
        div("card-header py-3") {
            h6("m-0 font-weight-bold text-primary") {
                +props.tableHeader
            }
        }
        div("card-body") {
            div("table-responsive") {
                table("table table-bordered") {
                    spread(tableInstance.getTableProps())
                    attrs["width"] = "100%"
                    attrs["cellSpacing"] = "0"
                    thead {
                        tableInstance.headerGroups.map { headerGroup ->
                            tr {
                                spread(headerGroup.getHeaderGroupProps())
                                headerGroup.headers.map { column ->
                                    val columnProps = column.getHeaderProps(column.getSortByToggleProps())
                                    th(classes = columnProps.className) {
                                        spread(columnProps)
                                        +column.render("Header")
                                        span {
                                            +when {
                                                column.isSorted -> " 🔽"
                                                column.isSortedDesc -> " 🔼"
                                                else -> ""
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    tbody {
                        spread(tableInstance.getTableBodyProps())
                        tableInstance.rows.map { row ->
                            tableInstance.prepareRow(row)
                            tr {
                                spread(row.getRowProps())
                                row.cells.map { cell ->
                                    // fixme: userProps are not present in actual html, but .render("Cell") produces td, so can't wrap
                                    child(cell.render("Cell", userProps = json().apply {
                                        spread(cell.getCellProps()) { key, value ->
                                            this[key] = value
                                        }
                                    }))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
