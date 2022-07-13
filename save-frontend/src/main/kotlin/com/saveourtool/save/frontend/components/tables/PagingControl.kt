/**
 * Paging control utils
 */

package com.saveourtool.save.frontend.components.tables

import csstype.ClassName
import react.ChildrenBuilder
import react.StateSetter
import react.dom.aria.ariaDescribedBy
import react.dom.button
import react.dom.div
import react.dom.em
import react.dom.form
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.em
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.input
import react.dom.option
import react.dom.select
import react.table.TableInstance

import kotlinx.html.hidden

/**
 * @param tableInstance
 * @param setPageIndex
 * @return set entries block
 */
fun <D : Any> ChildrenBuilder.setEntries(tableInstance: TableInstance<D>, setPageIndex: StateSetter<Int>) = div {
    className = ClassName("row mt-3")
    div {
        className = ClassName("col-0 pt-1 pr-0")
        +"Show "
    }
    div {
        className = ClassName("col-5 pr-0")
        div {
            className = ClassName("input-group-sm input-group")
            select {
                className = ClassName("form-control")
                listOf("10", "25", "50", "100").forEach {
                    option {
                        className = ClassName("list-group-item")
                        val entries = it
                        value = entries
                        +entries
                    }
                }
                onChange = {
                    val entries = it.target.value
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, 0)
                    tableInstance.setPageSize(entries.toInt())
                }
            }
        }
    }
    div {
        className = ClassName("col-0 pt-1 pl-2")
        +" entries"
    }
}

/**
 * @param tableInstance
 * @param setPageIndex
 * @param pageIndex
 * @param pageCount
 * @return paging control block
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun <D : Any> ChildrenBuilder.pagingControl(
    tableInstance: TableInstance<D>,
    setPageIndex: StateSetter<Int>,
    pageIndex: Int,
    pageCount: Int,
) =
        div {
            className = ClassName("row")
            // First page
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, 0)
                }
                disabled = !tableInstance.canPreviousPage
                +js("String.fromCharCode(171)").unsafeCast<String>()
            }
            // Previous page icon <
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageIndex - 1)
                }
                disabled = !tableInstance.canPreviousPage
                +js("String.fromCharCode(8249)").unsafeCast<String>()
            }
            // Previous before previous page
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                val index = pageIndex - 2
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, index)
                }
                hidden = (index < 0)
                em {
                    +"${index + 1}"
                }
            }
            // Previous page number
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageIndex - 1)
                }
                hidden = !tableInstance.canPreviousPage
                em {
                    +pageIndex.toString()
                }
            }
            // Current page number
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                disabled = true
                em {
                    +"${pageIndex + 1}"
                }
            }
            // Next page number
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageIndex + 1)
                }
                hidden = !tableInstance.canNextPage
                em {
                    +"${pageIndex + 2}"
                }
            }
            // Next after next page
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                val index = pageIndex + 2
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, index)
                }
                hidden = (index > pageCount - 1)
                em {
                    +"${index + 1}"
                }
            }
            // Next page icon >
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageIndex + 1)
                }
                disabled = !tableInstance.canNextPage
                +js("String.fromCharCode(8250)").unsafeCast<String>()
            }
            // Last page
            button {
                type = ButtonType.button
                className = ClassName("btn btn-link")
                onClick = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageCount - 1)
                }
                disabled = !tableInstance.canNextPage
                +js("String.fromCharCode(187)").unsafeCast<String>()
            }
            // Jump to the concrete page
            jumpToPage(tableInstance, setPageIndex, pageCount)

            setEntries(tableInstance, setPageIndex)
        }

/**
 * @param tableInstance
 * @param setPageIndex
 * @param pageCount
 * @return jump to page block
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun <D : Any> ChildrenBuilder.jumpToPage(tableInstance: TableInstance<D>, setPageIndex: StateSetter<Int>, pageCount: Int) =
        form {
            var number = 0
            div {
                className = ClassName("row")
                div {
                    className = ClassName("col-7 pr-0")
                    div {
                        className = ClassName("input-group input-group-sm mb-3 mt-3")
                        input {
                            type = InputType.text
                            className = ClassName("form-control")
                            ariaDescribedBy = "basic-addon2"
                            placeholder = "Jump to the page"
                            onChange = {
                                // TODO: Provide validation of non int types
                                number = it.target.value.toInt() - 1
                                if (number < 0) {
                                    number = 0
                                }
                                if (number > pageCount - 1) {
                                    number = pageCount - 1
                                }
                            }
                        }
                    }
                }

                div {
                    className = ClassName("col-sm-offset-10 mr-3 justify-content-start")
                    div {
                        className = ClassName("input-group input-group-sm mb-6")
                        div {
                            className = ClassName("input-group-append mt-3")
                            button {
                                type = ButtonType.submit
                                className = ClassName("btn btn-outline-secondary")
                                onClick = {
                                    setPageIndexAndGoToPage(tableInstance, setPageIndex, number)
                                }
                                +js("String.fromCharCode(10143)").unsafeCast<String>()
                            }
                        }
                    }
                }
            }
        }

private fun <D : Any> setPageIndexAndGoToPage(
    tableInstance: TableInstance<D>,
    setPageIndex: StateSetter<Int>,
    index: Int
) {
    setPageIndex(index)
    tableInstance.gotoPage(index)
}
