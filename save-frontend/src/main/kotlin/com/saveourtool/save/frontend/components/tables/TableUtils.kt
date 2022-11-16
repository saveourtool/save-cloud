package com.saveourtool.save.frontend.components.tables

import tanstack.table.core.CellContext
import tanstack.table.core.RowData

val <TData: RowData, TValue> CellContext<TData, TValue>.value get() = this.getValue()

val <TData: RowData, TValue> CellContext<TData, TValue>.pageIndex get() = this.table.getState().pagination.pageIndex

val <TData: RowData, TValue> CellContext<TData, TValue>.pageSize get() = this.table.getState().pagination.pageSize
