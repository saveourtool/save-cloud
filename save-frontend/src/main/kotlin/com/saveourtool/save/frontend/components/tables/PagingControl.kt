/**
 * Paging control utils
 */

package com.saveourtool.save.frontend.components.tables

import com.saveourtool.save.frontend.externals.formik.Form
import com.saveourtool.save.frontend.externals.formik.Formik
import com.saveourtool.save.frontend.externals.formik.FormikConfig
import com.saveourtool.save.frontend.externals.formik.FormikProps
import csstype.ClassName
import org.w3c.dom.HTMLSelectElement
import react.StateSetter
import react.dom.RDOMBuilder
import react.dom.attrs
import react.dom.button
import react.dom.div
import react.dom.em
import react.dom.option
import react.dom.select
import react.table.TableInstance

import kotlinx.html.ButtonType
import kotlinx.html.Tag
import kotlinx.html.hidden
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.js.jso
import react.ElementType
import react.FC
import react.Props
import react.PropsWithChildren
import react.ReactNode
import react.create
import react.createElement
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.span
import react.react
import react.useState

/**
 * @param tableInstance
 * @param setPageIndex
 * @return set entries block
 */
fun <T : Tag, D : Any> RDOMBuilder<T>.setEntries(tableInstance: TableInstance<D>, setPageIndex: StateSetter<Int>) = div("row mt-3") {
    div("col-0 pt-1 pr-0") {
        +"Show "
    }
    div("col-5 pr-0") {
        div("input-group-sm input-group") {
            select(classes = "form-control") {
                listOf("10", "25", "50", "100").forEach {
                    option("list-group-item") {
                        val entries = it
                        attrs.value = entries
                        +entries
                    }
                }
                attrs.onChangeFunction = {
                    val tg = it.target as HTMLSelectElement
                    val entries = tg.value
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, 0)
                    tableInstance.setPageSize(entries.toInt())
                }
            }
        }
    }
    div("col-0 pt-1 pl-2") {
        +" entries"
    }
}

val jumpToPageComponent = jumpToPage()

/**
 * @param tableInstance
 * @param setPageIndex
 * @param pageIndex
 * @param pageCount
 * @return paging control block
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun <T : Tag, D : Any> RDOMBuilder<T>.pagingControl(
    tableInstance: TableInstance<D>,
    setPageIndex: StateSetter<Int>,
    pageIndex: Int,
    pageCount: Int,
) =
        div("row") {
            // First page
            button(type = ButtonType.button, classes = "btn btn-link") {
                attrs.onClickFunction = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, 0)
                }
                attrs.disabled = !tableInstance.canPreviousPage
                +js("String.fromCharCode(171)").unsafeCast<String>()
            }
            // Previous page icon <
            button(type = ButtonType.button, classes = "btn btn-link") {
                attrs.onClickFunction = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageIndex - 1)
                }
                attrs.disabled = !tableInstance.canPreviousPage
                +js("String.fromCharCode(8249)").unsafeCast<String>()
            }
            // Previous before previous page
            button(type = ButtonType.button, classes = "btn btn-link") {
                val index = pageIndex - 2
                attrs.onClickFunction = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, index)
                }
                attrs.hidden = (index < 0)
                em {
                    +"${index + 1}"
                }
            }
            // Previous page number
            button(type = ButtonType.button, classes = "btn btn-link") {
                attrs.onClickFunction = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageIndex - 1)
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
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageIndex + 1)
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
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, index)
                }
                attrs.hidden = (index > pageCount - 1)
                em {
                    +"${index + 1}"
                }
            }
            // Next page icon >
            button(type = ButtonType.button, classes = "btn btn-link") {
                attrs.onClickFunction = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageIndex + 1)
                }
                attrs.disabled = !tableInstance.canNextPage
                +js("String.fromCharCode(8250)").unsafeCast<String>()
            }
            // Last page
            button(type = ButtonType.button, classes = "btn btn-link") {
                attrs.onClickFunction = {
                    setPageIndexAndGoToPage(tableInstance, setPageIndex, pageCount - 1)
                }
                attrs.disabled = !tableInstance.canNextPage
                +js("String.fromCharCode(187)").unsafeCast<String>()
            }
            // Jump to the concrete page
            child(jumpToPageComponent, jso {
                this.tableInstance = tableInstance
                this.setPageIndex = setPageIndex
                this.pageCount = pageCount
            })

            setEntries(tableInstance, setPageIndex)
        }

external interface JumpToPageProps : Props {
    var tableInstance: TableInstance<*>?
    var setPageIndex: StateSetter<Int>?
    var pageCount: Int?
}

/**
 * @return jump to page block
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun jumpToPage() = FC<JumpToPageProps> { props ->
    val (number, setNumber) = useState(0)
    +Formik(jso {
        initialValues = jso {
            this.number = 0
        }
        asDynamic().validate = { values: dynamic ->
            if (values.number < 0) {
                jso { this.number = "Can't be negative" }
            } else if (number > props.pageCount!! - 1) {
                jso { this.number = "Can't exceed page count" }
            } else {
                jso<dynamic> {}
            }
        }
        children = { formikProps ->
            ReactNode(arrayOf(
                Form::class.react.create {
                    div {
                        className = ClassName("row")
                        div {
                            className = ClassName("col-7 pr-0")
                            div {
                                className = ClassName("input-group input-group-sm mb-3 mt-3")
                                input {
                                    className = ClassName("form-control")
                                    type = InputType.text
                                    id = "number"
                                    asDynamic()["aria-describedby"] = "basic-addon2"
                                    asDynamic().placeholder = "Jump to the page"
                                    value = formikProps.values.number
                                    onChange = {
                                        // TODO: Provide validation of non int types
                                        val tg = it.target
                                        setNumber(tg.value.toInt())
                                        formikProps.handleChange(it)
                                    }
                                }
                            }
                        }
                    }
                },

                div.create {
                    className = ClassName("col-sm-offset-10 mr-3 justify-content-start")
                    div {
                        className = ClassName("input-group input-group-sm mb-6")
                        div {
                            className = ClassName("input-group-append mt-3")
                            button {
                                className = ClassName("btn btn-outline-secondary")
                                type = react.dom.html.ButtonType.submit
                                onClick = {
                                    setPageIndexAndGoToPage(props.tableInstance!!, props.setPageIndex!!, number)
                                }
                                +js("String.fromCharCode(10143)").unsafeCast<String>()
                            }
                        }
                    }
                })
            )
        }
    })
}

private fun <D : Any> setPageIndexAndGoToPage(
    tableInstance: TableInstance<D>,
    setPageIndex: StateSetter<Int>,
    index: Int
) {
    setPageIndex(index)
    tableInstance.gotoPage(index)
}
