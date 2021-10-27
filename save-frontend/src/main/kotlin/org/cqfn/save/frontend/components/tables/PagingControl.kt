package org.cqfn.save.frontend.components.tables

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.hidden
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.StateSetter
import react.dom.RDOMBuilder
import react.dom.attrs
import react.dom.button
import react.dom.div
import react.dom.em
import react.dom.form
import react.dom.input
import react.dom.option
import react.dom.select
import react.table.TableInstance

fun <T : Tag, D : Any> RDOMBuilder<T>.setEntries(tableInstance: TableInstance<D>, setPageIndex: StateSetter<Int>) = div("row") {
    div("col-0 pt-3 pl-3 pr-0") {
        +"Show "
    }
    div("col-1 pr-0") {
        div("input-group input-group-sm mb-3 mt-3") {
            select(classes = "form-control") {
                option("list-group-item") {
                    val entries = "10"
                    attrs.value = entries
                    +entries
                }
                option("list-group-item") {
                    val entries = "25"
                    attrs.value = entries
                    +entries
                }
                option("list-group-item") {
                    val entries = "50"
                    attrs.value = entries
                    +entries
                }
                option("list-group-item") {
                    val entries = "100"
                    attrs.value = entries
                    +entries
                }
                attrs.onChangeFunction = {
                    val tg = it.target as HTMLSelectElement
                    val entries = tg.value
                    setPageIndex(0)
                    tableInstance.gotoPage(0)
                    tableInstance.setPageSize(entries.toInt())
                }
            }
        }
    }
    div("col-0 pt-3 pl-2") {
        +" entries"
    }
}

fun <T : Tag, D : Any> RDOMBuilder<T>.pagingControl(
    tableInstance: TableInstance<D>,
    setPageIndex: StateSetter<Int>,
    pageIndex: Int,
    pageCount: Int) =
    div("row") {
        // First page
        button(type = ButtonType.button, classes = "btn btn-link") {
            attrs.onClickFunction = {
                setPageIndex(0)
                tableInstance.gotoPage(0)
            }
            attrs.disabled = !tableInstance.canPreviousPage
            +js("String.fromCharCode(171)").unsafeCast<String>()
        }
        // Previous page icon <
        button(type = ButtonType.button, classes = "btn btn-link") {
            attrs.onClickFunction = {
                setPageIndex(pageIndex - 1)
                tableInstance.previousPage()
            }
            attrs.disabled = !tableInstance.canPreviousPage
            +js("String.fromCharCode(8249)").unsafeCast<String>()
        }
        // Previous before previous page
        button(type = ButtonType.button, classes = "btn btn-link") {
            val index = pageIndex - 2
            attrs.onClickFunction = {
                setPageIndex(index)
                tableInstance.gotoPage(index)
            }
            attrs.hidden = (index < 0)
            em {
                +"${index + 1}"
            }
        }
        // Previous page number
        button(type = ButtonType.button, classes = "btn btn-link") {
            attrs.onClickFunction = {
                setPageIndex(pageIndex - 1)
                tableInstance.previousPage()
            }
            attrs.hidden = !tableInstance.canPreviousPage
            em {
                +pageIndex.toString()
            }
        }
        // Current page number
        button(type = ButtonType.button, classes = "btn btn-link") {
            attrs.disabled = true
            em {
                +"${pageIndex + 1}"
            }
        }
        // Next page number
        button(type = ButtonType.button, classes = "btn btn-link") {
            attrs.onClickFunction = {
                setPageIndex(pageIndex + 1)
                tableInstance.nextPage()
            }
            attrs.hidden = !tableInstance.canNextPage
            em {
                +"${pageIndex + 2}"
            }
        }
        // Next after next page
        button(type = ButtonType.button, classes = "btn btn-link") {
            val index = pageIndex + 2
            attrs.onClickFunction = {
                setPageIndex(index)
                tableInstance.gotoPage(index)
            }
            attrs.hidden = (index > pageCount - 1)
            em {
                +"${index + 1}"
            }
        }
        // Next page icon >
        button(type = ButtonType.button, classes = "btn btn-link") {
            attrs.onClickFunction = {
                setPageIndex(pageIndex + 1)
                tableInstance.nextPage()
            }
            attrs.disabled = !tableInstance.canNextPage
            +js("String.fromCharCode(8250)").unsafeCast<String>()
        }
        // Last page
        button(type = ButtonType.button, classes = "btn btn-link") {
            attrs.onClickFunction = {
                setPageIndex(pageCount - 1)
                tableInstance.gotoPage(pageCount - 1)
            }
            attrs.disabled = !tableInstance.canNextPage
            +js("String.fromCharCode(187)").unsafeCast<String>()
        }
        // Jump to the concrete page
        jumpToPage(tableInstance, setPageIndex, pageCount)
    }

fun <T : Tag, D : Any> RDOMBuilder<T>.jumpToPage(tableInstance: TableInstance<D>, setPageIndex: StateSetter<Int>, pageCount: Int) =
    form {
        var number = 0
        div("row") {
            div("col-8 pr-0") {
                div("input-group input-group-sm mb-3 mt-3") {
                    input(type = InputType.text, classes = "form-control") {
                        attrs["aria-describedby"] = "basic-addon2"
                        attrs.placeholder = "Jump to the page"
                        attrs {
                            onChangeFunction = {
                                // TODO: Provide validation of non int types
                                val tg = it.target as HTMLInputElement
                                number = tg.value.toInt() - 1
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
            }

            div("col-sm-offset-10 mr-3 justify-content-start") {
                div("input-group input-group-sm mb-6") {
                    div("input-group-append mt-3") {
                        button(type = ButtonType.submit, classes = "btn btn-outline-secondary") {
                            attrs.onClickFunction = {
                                setPageIndex(number)
                                tableInstance.gotoPage(number)
                            }
                            +js("String.fromCharCode(10143)").unsafeCast<String>()
                        }
                    }
                }
            }
        }
    }
