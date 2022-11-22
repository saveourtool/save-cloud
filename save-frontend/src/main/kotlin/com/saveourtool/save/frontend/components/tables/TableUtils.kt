package com.saveourtool.save.frontend.components.tables

import react.StateSetter
import tanstack.table.core.CellContext
import tanstack.table.core.RowData
import tanstack.table.core.Table
import tanstack.table.core.Updater

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
