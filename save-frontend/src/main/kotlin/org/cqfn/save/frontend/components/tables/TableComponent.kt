/**
 * Utilities for react-tables
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package org.cqfn.save.frontend.components.tables

import org.cqfn.save.frontend.components.modal.errorModal
import org.cqfn.save.frontend.utils.spread

import kotlinext.js.jsObject
import react.PropsWithChildren
import react.RBuilder
import react.dom.button
import react.dom.div
import react.dom.em
import react.dom.h6
import react.dom.span
import react.dom.table
import react.dom.tbody
import react.dom.th
import react.dom.thead
import react.dom.tr
import react.fc
import react.table.Column
import react.table.PluginHook
import react.table.Row
import react.table.TableInstance
import react.table.TableOptions
import react.table.TableRowProps
import react.table.usePagination
import react.table.useSortBy
import react.table.useTable
import react.useEffect
import react.useMemo
import react.useState

import kotlin.js.json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * [RProps] of a data table
 */
external interface TableProps : PropsWithChildren {
    /**
     * Table header
     */
    var tableHeader: String
}

/**
 * A `RComponent` for a data table
 *
 * @param columns columns as an array of [Column]
 * @param getData a function to retrieve data for the table, returns an array of data of type [out D] that will be inserted into the table
 * @param initialPageSize initial size of table page
 * @param getRowProps a function returning `TableRowProps` for customization of table row, defaults to empty
 * @param useServerPaging whether data is split into pages server-side or in browser
 * @param getPageCount a function to retrieve number of pages of data, is [useServerPaging] is `true`
 * @param usePageSelection whether to display entries settings
 * @param plugins
 * @param additionalOptions
 * @param renderExpandedRow how to render an expanded row if `useExpanded` plugin is used
 * @return a functional react component
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress(
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
    "TYPE_ALIAS",
    "ForbiddenComment",
    "LongMethod",
    "LongParameterList",
    "TooGenericExceptionCaught"
)
fun <D : Any> tableComponent(
    columns: Array<out Column<D, *>>,
    initialPageSize: Int = 10,
    useServerPaging: Boolean = false,
    usePageSelection: Boolean = false,
    plugins: Array<PluginHook<D>> = arrayOf(useSortBy, usePagination),
    additionalOptions: TableOptions<D>.() -> Unit = {},
    getRowProps: ((Row<D>) -> TableRowProps) = { jsObject() },
    getPageCount: (suspend (pageSize: Int) -> Int)? = null,
    renderExpandedRow: (RBuilder.(table: TableInstance<D>, row: Row<D>) -> Unit)? = undefined,
    getData: suspend (pageIndex: Int, pageSize: Int) -> Array<out D>,
) = fc<TableProps> { props ->
    require(useServerPaging xor (getPageCount == null)) {
        "Either use client-side paging or provide a function to get page count"
    }

    val (data, setData) = useState<Array<out D>>(emptyArray())
    val (pageCount, setPageCount) = useState(1)
    val (pageIndex, setPageIndex) = useState(0)
    val (isModalOpen, setIsModalOpen) = useState(false)
    val (dataAccessException, setDataAccessException) = useState<Exception?>(null)

    val tableInstance: TableInstance<D> = useTable(options = jsObject {
        this.columns = useMemo { columns }
        this.data = data
        this.manualPagination = useServerPaging
        if (useServerPaging) {
            this.pageCount = pageCount
        }
        this.initialState = jsObject {
            this.pageSize = initialPageSize
            this.pageIndex = pageIndex
        }
        additionalOptions()
    }, plugins = plugins)

    useEffect(arrayOf<dynamic>(tableInstance.state.pageSize, pageCount)) {
        if (useServerPaging) {
            val pageCountDeferred = GlobalScope.async {
                getPageCount!!.invoke(tableInstance.state.pageSize)
            }
            pageCountDeferred.invokeOnCompletion {
                setPageCount(pageCountDeferred.getCompleted())
            }
        }
    }

    // list of entities, updates of which will cause update of the data retrieving effect
    val dependencies: Array<dynamic> = if (useServerPaging) {
        arrayOf(tableInstance.state.pageIndex, tableInstance.state.pageSize, pageCount)
    } else {
        // when all data is already available, we don't need to repeat `getData` calls
        emptyArray()
    }
    useEffect(*dependencies) {
        GlobalScope.launch {
            try {
                setData(getData(tableInstance.state.pageIndex, tableInstance.state.pageSize))
            } catch (e: Exception) {
                setIsModalOpen(true)
                setDataAccessException(e)
            }
        }
    }

    if (usePageSelection) {
        div {
            setEntries(tableInstance, setPageIndex)
        }
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
                        tableInstance.page.map { row ->
                            tableInstance.prepareRow(row)
                            tr {
                                spread(row.getRowProps(getRowProps(row)))
                                row.cells.map { cell ->
                                    // fixme: userProps are not present in actual html, but .render("Cell") produces td, so can't wrap
                                    child(cell.render("Cell", userProps = json().apply {
                                        spread(cell.getCellProps()) { key, value ->
                                            this[key] = value
                                        }
                                    }))
                                }
                            }
                            if (row.isExpanded) {
                                requireNotNull(renderExpandedRow) {
                                    "`useExpanded` is used, but no method for expanded row is provided"
                                }
                                renderExpandedRow.invoke(this@tbody, tableInstance, row)
                            }
                        }
                    }
                }
                if (tableInstance.pageCount > 1) {
                    // block with paging controls
                    div("wrapper container m-0 p-0") {
                        pagingControl(tableInstance, setPageIndex, pageIndex, pageCount)
                    }
                    div {
                        +"Page "
                        em {
                            +"${tableInstance.state.pageIndex + 1} of ${tableInstance.pageCount}"
                        }
                    }
                }
            }
        }
    }
    errorModal(
        "Error",
        "Error when fetching data: ${dataAccessException?.message}",
        {
            attrs.isOpen = isModalOpen
        }) {
        setIsModalOpen(false)
    }
}
