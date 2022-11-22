package com.saveourtool.save.frontend.components.tables

import kotlinx.js.ReadonlyArray
import kotlinx.js.jso
import react.ReactNode
import tanstack.table.core.CellContext
import tanstack.table.core.ColumnDef
import tanstack.table.core.ColumnDefTemplate
import tanstack.table.core.RowData
import tanstack.table.core.StringOrTemplateHeader

fun <TData : RowData> columns(
    block: ColumnBuilder<TData>.() -> Unit,
): ReadonlyArray<ColumnDef<TData, *>> =
    ColumnBuilder<TData>().apply(block).build()


/**
 * Inspired by (meaning copy-pasted) `ColumnBuilder` from `kotlin-react-table`,
 * which doesn't exist in `kotlin-tanstack-react-table`.
 */
class ColumnBuilder<TData : RowData> {
    private val columns = mutableListOf<ColumnDef<TData, *>>()

    fun column(
        id: String,
        header: String,
        render: (cellContext: CellContext<TData, TData>) -> ReactNode,
    ) = column(id, header, { this }, render)

    fun <TValue> column(
        id: String,
        header: String,
        accessor: TData.() -> TValue,
        render: (cellContext: CellContext<TData, TValue>) -> ReactNode,
    ): ColumnDef<TData, TValue> {
        return jso<ColumnDef<TData, TValue>> {
            this.id = id
            this.header = StringOrTemplateHeader(header)
            this.accessorFn = { d, _ -> accessor(d) }
            this.cell = ColumnDefTemplate { cellCtx ->
                render(cellCtx)
            }
        }
            .also { columns.add(it) }
    }

    fun build(): ReadonlyArray<ColumnDef<TData, *>> =
        columns.toTypedArray()
}
