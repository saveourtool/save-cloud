package com.saveourtool.save.frontend.components.tables

import kotlinx.js.jso
import react.ChildrenBuilder
import react.FC
import react.Props
import react.StateSetter
import react.useState
import tanstack.table.core.CellContext
import tanstack.table.core.ExpandedState
import tanstack.table.core.RowData
import tanstack.table.core.Table
import tanstack.table.core.TableOptions
import tanstack.table.core.TableState
import tanstack.table.core.Updater
import tanstack.table.core.getExpandedRowModel

val <TData: RowData, TValue> CellContext<TData, TValue>.value: TValue get() = this.getValue()

val <TData: RowData, TValue> CellContext<TData, TValue>.pageIndex get() = this.table.getState().pagination.pageIndex

val <TData: RowData, TValue> CellContext<TData, TValue>.pageSize get() = this.table.getState().pagination.pageSize

fun <TData: RowData> Table<TData>.visibleColumnsCount() = this.getVisibleFlatColumns().size

fun <T> StateSetter<T>.invoke(updaterOrValue: Updater<T>) =
    if (jsTypeOf(updaterOrValue) == "function") {
        this.invoke(updaterOrValue.unsafeCast<(T) -> T>())
    } else {
        this.invoke(updaterOrValue.unsafeCast<T>())
    }

fun <D : RowData> ChildrenBuilder.enableExpanding(tableOptions: TableOptions<D>) {
    val (expanded, setExpanded) = useState<ExpandedState>(jso {})
    tableOptions.initialState!!.expanded = expanded
    tableOptions.asDynamic().state.unsafeCast<TableState>().expanded = expanded
    tableOptions.onExpandedChange = { setExpanded.invoke(it) }
    tableOptions.getExpandedRowModel = getExpandedRowModel()
}
