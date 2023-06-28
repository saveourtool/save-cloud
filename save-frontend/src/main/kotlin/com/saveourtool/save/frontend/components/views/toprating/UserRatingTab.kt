@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views.toprating

import com.saveourtool.save.frontend.components.tables.*
import com.saveourtool.save.frontend.externals.fontawesome.faTrophy
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.v1
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.FC
import react.Fragment
import react.Props
import react.create
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import web.cssom.*

val userRatingTable: FC<Props> = FC { _ ->

    @Suppress(
        "TOO_MANY_LINES_IN_LAMBDA",
        "MAGIC_NUMBER",
    )
    val userRatingTable: FC<TableProps<UserInfo>> = tableComponent(
        columns = {
            columns {
                column(id = "index", header = "Position") { cellContext ->
                    Fragment.create {
                        td {
                            val index = cellContext.row.index + 1 + cellContext.pageIndex * cellContext.pageSize
                            var isTrophy = false
                            var newColor = ""
                            when (index) {
                                1 -> {
                                    isTrophy = true
                                    newColor = "#ebcc36"
                                }
                                2 -> {
                                    isTrophy = true
                                    newColor = "#7d7d7d"
                                }
                                3 -> {
                                    isTrophy = true
                                    newColor = "#a15703"
                                }
                            }
                            if (isTrophy) {
                                style = jso {
                                    color = newColor.unsafeCast<Color>()
                                }
                                fontAwesomeIcon(icon = faTrophy)
                            }
                            +" $index"
                        }
                    }
                }
                column(id = "name", header = "Name", { name }) { cellContext ->
                    Fragment.create {
                        td {
                            Link {
                                img {
                                    className =
                                            ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                    src = cellContext.row.original.avatar?.let {
                                        "/api/$v1/avatar$it"
                                    } ?: "img/undraw_profile.svg"
                                    style = jso {
                                        height = 2.rem
                                        width = 2.rem
                                    }
                                }
                                to = "/${FrontendRoutes.PROFILE.path}/${cellContext.value}"
                                +" ${cellContext.value}"
                            }
                        }
                    }
                }
                column(id = "rating", header = "Rating") { cellContext ->
                    Fragment.create {
                        td {
                            +cellContext.value.rating.toString()
                        }
                    }
                }
            }
        },
        initialPageSize = 10,
        useServerPaging = false,
        isTransparentGrid = true,
    )

    div {
        className = ClassName("row justify-content-center")
        div {
            className = ClassName("col-8")
            userRatingTable {
                getData = { _, _ ->
                    get(
                        url = "$apiUrl/users/all",
                        headers = jsonHeaders,
                        loadingHandler = ::noopLoadingHandler,
                        responseHandler = ::noopResponseHandler,
                    ).unsafeMap {
                        it.decodeFromJsonString<Array<UserInfo>>()
                    }.let { array ->
                        array.sortByDescending { it.rating }
                        array
                    }
                }
            }
        }
    }
}