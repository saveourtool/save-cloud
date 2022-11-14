/**
 * Utilities for react-tables
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.save.frontend.components.tables

import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.http.HttpStatusException
import com.saveourtool.save.frontend.utils.WithRequestStatusContext
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.frontend.utils.spread
import csstype.ClassName

import org.w3c.fetch.Response
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.em
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import react.table.PluginHook
import react.table.TableRowProps
import react.table.usePagination
import react.table.useSortBy
import tanstack.react.table.useReactTable
import tanstack.table.core.Column
import tanstack.table.core.Table
import tanstack.table.core.Row
import tanstack.table.core.TableOptions

import kotlin.js.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.js.jso
import tanstack.table.core.ColumnDef

/**
 * [Props] of a data table
 */
external interface TableProps<D : Any> : Props {
    /**
     * Table header
     */
    var tableHeader: String

    /**
     * Lambda to get table data
     */
    @Suppress("TYPE_ALIAS")
    var getData: suspend WithRequestStatusContext.(pageIndex: Int, pageSize: Int) -> Array<out D>

    /**
     * Lambda to update number of pages
     */
    var getPageCount: (suspend (pageSize: Int) -> Int)?
}

/**
 * A `Component` for a data table
 *
 * @param columns columns as an array of [Column]
 * @param initialPageSize initial size of table page
 * @param getRowProps a function returning `TableRowProps` for customization of table row, defaults to empty
 * @param useServerPaging whether data is split into pages server-side or in browser
 * @param usePageSelection whether to display entries settings
 * @param plugins
 * @param additionalOptions
 * @param renderExpandedRow how to render an expanded row if `useExpanded` plugin is used. Is invoked inside a `<tbody>` tag.
 * @param commonHeader (optional) a common header for the table, which will be placed above individual column headers
 * @param getAdditionalDependencies allows filter the table using additional components (dependencies)
 * @param isTransparentGrid
 * @return a functional react component
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
    "TYPE_ALIAS",
    "ComplexMethod",
    "ForbiddenComment",
    "LongMethod",
    "LongParameterList",
    "TooGenericExceptionCaught",
    "MAGIC_NUMBER",
    "LAMBDA_IS_NOT_LAST_PARAMETER"
)
fun <D : Any, P : TableProps<D>> tableComponent(
    columns: (P) -> Array<out ColumnDef<D, *>>,
    initialPageSize: Int = 10,
    useServerPaging: Boolean = false,
    usePageSelection: Boolean = false,
    isTransparentGrid: Boolean = false,
    plugins: Array<PluginHook<D>> = arrayOf(useSortBy, usePagination),
    additionalOptions: TableOptions<D>.() -> Unit = {},
    getRowProps: ((Row<D>) -> TableRowProps) = { jso() },
    renderExpandedRow: (ChildrenBuilder.(table: Table<D>, row: Row<D>) -> Unit)? = undefined,
    commonHeader: ChildrenBuilder.(table: Table<D>) -> Unit = {},
    getAdditionalDependencies: (P) -> Array<dynamic> = { emptyArray() },
): FC<P> = FC { props ->
    require(useServerPaging xor (props.getPageCount == null)) {
        "Either use client-side paging or provide a function to get page count"
    }
    val (data, setData) = useState<Array<out D>>(emptyArray())
    val (pageCount, setPageCount) = useState(1)
    val (pageIndex, setPageIndex) = useState(0)
    val (isModalOpen, setIsModalOpen) = useState(false)
    val (dataAccessException, setDataAccessException) = useState<Exception?>(null)
    val scope = CoroutineScope(Dispatchers.Default)

    val tableInstance: Table<D> = useReactTable(options = jso {
        this.columns = useMemo { columns(props) }
        this.data = data
        this.manualPagination = useServerPaging
        if (useServerPaging) {
            this.pageCount = pageCount
        }
        this.initialState = jso {
            this.pagination = jso {
                this.pageSize = initialPageSize
                this.pageIndex = pageIndex
            }
        }
        additionalOptions()
    }, plugins = plugins)

    // list of entities, updates of which will cause update of the data retrieving effect
    val dependencies: Array<dynamic> = if (useServerPaging) {
        arrayOf(tableInstance.getState().pagination.pageIndex, tableInstance.getState().pagination.pageSize, pageCount)
    } else {
        // when all data is already available, we don't need to repeat `getData` calls
        emptyArray()
    } + getAdditionalDependencies(props)

    useEffect(*dependencies) {
        if (useServerPaging) {
            scope.launch {
                val newPageCount = props.getPageCount!!.invoke(tableInstance.getState().pagination.pageSize)
                if (newPageCount != pageCount) {
                    setPageCount(newPageCount)
                }
            }
        }
    }

    val statusContext = useContext(requestStatusContext)
    val context = object : WithRequestStatusContext {
        override val coroutineScope = CoroutineScope(Dispatchers.Default)
        override fun setResponse(response: Response) = statusContext.setResponse(response)
        override fun setRedirectToFallbackView(isNeedRedirect: Boolean, response: Response) = statusContext.setRedirectToFallbackView(
            isNeedRedirect && response.status == 404.toShort()
        )
        override fun setLoadingCounter(transform: (oldValue: Int) -> Int) = statusContext.setLoadingCounter(transform)
    }
    useEffect(*dependencies) {
        scope.launch {
            try {
                setData(context.(props.getData)(
                    tableInstance.getState().pagination.pageIndex, tableInstance.getState().pagination.pageSize
                ))
            } catch (e: CancellationException) {
                // this means, that view is re-rendering while network request was still in progress
                // no need to display an error message in this case
            } catch (e: HttpStatusException) {
                // this is a normal situation which should be handled by responseHandler in `getData` itself.
                // no need to display an error message in this case
            } catch (e: Exception) {
                // other exceptions are not handled by `responseHandler` and should be displayed separately
                setIsModalOpen(true)
                setDataAccessException(e)
            }
        }
        cleanup {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }

    div {
        className = ClassName("${if (isTransparentGrid) "" else "card shadow"} mb-4")
        div {
            className = ClassName("card-header py-3")
            h6 {
                className = ClassName("m-0 font-weight-bold text-primary text-center")
                +props.tableHeader
            }
        }
        div {
            className = ClassName("card-body")
            div {
                className = ClassName("table-responsive")
                table {
                    className = ClassName("table ${if (isTransparentGrid) "" else "table-bordered"}")
//                    spread(tableInstance.getTableProps())
                    width = 100.0
                    cellSpacing = "0"
                    thead {
                        commonHeader(tableInstance)
                        tableInstance.getHeaderGroups().map { headerGroup ->
                            tr {
                                id = headerGroup.id
//                                spread(headerGroup.getHeaderGroupProps())
                                headerGroup.headers.map { column ->
                                    val columnProps = column.getHeaderProps(column.getSortByToggleProps())
                                    val className = if (column.canSort) columnProps.className else ClassName("")
                                    th {
                                        this.className = className
                                        +column.render("Header")
                                        // fixme: find a way to set `canSort`; now it's always true
                                        if (column.canSort) {
                                            spread(columnProps)
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

                if (data.isEmpty()) {
                    div {
                        className = ClassName("align-items-center justify-content-center mb-4")
                        h6 {
                            className = ClassName("m-0 font-weight-bold text-primary text-center")
                            +"No results found"
                        }
                    }
                }

                div {
                    className = ClassName("wrapper container m-0 p-0")
                    pagingControl(tableInstance, setPageIndex, pageIndex, pageCount)

                    div {
                        className = ClassName("row ml-1")
                        +"Page "
                        em {
                            +"${tableInstance.state.pageIndex + 1} of ${tableInstance.pageCount}"
                        }
                    }
                }
            }
        }
    }

    displayModal(
        isModalOpen,
        "Error",
        "Error when fetching data: ${dataAccessException?.message}",
        mediumTransparentModalStyle,
        { setIsModalOpen(false) },
    ) {
        buttonBuilder("Close", "secondary") {
            setIsModalOpen(false)
        }
    }
}
