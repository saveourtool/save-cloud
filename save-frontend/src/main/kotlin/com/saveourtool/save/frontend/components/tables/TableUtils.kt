package com.saveourtool.save.frontend.components.tables

import tanstack.table.core.CellContext
import tanstack.table.core.RowData

val <TData: RowData, TValue> CellContext<TData, TValue>.value get() = this.getValue()
