/**
 * Contests "market" - showcase for users, where they can navigate and check contests
 */

@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "MAGIC_NUMBER")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.LocalDateTime
import com.saveourtool.save.utils.getCurrentLocalDateTime

import csstype.ClassName
import csstype.rem
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main

import kotlinx.js.jso

/**
 * TODO:
 * 1. Хотите создавать контесты - напишите нам
 * 2. Добавить свой контест: выбираем организацию и дальше добавляем
 * 3. Гранд чемпионы SAVE
 * 4. Обратный отсчет
 * 5. Количество контествов и участников
 */

/**
 * [Props] retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ContestListViewProps : Props {
    var currentUserInfo: UserInfo?
}

/**
 * [State] of [ContestListView] component
 */
external interface ContestListViewState : State {
    /**
     * current time
     */
    var currentDateTime: LocalDateTime
}

/**
 * A view with collection of contests
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ContestListView : AbstractView<ContestListViewProps, ContestListViewState>() {
    init {
        state.currentDateTime = getCurrentLocalDateTime()
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    override fun ChildrenBuilder.render() {
        main {
            className = ClassName("main-content mt-0 ps")
            div {
                className = ClassName("page-header align-items-start min-vh-100")
                div {
                    className = ClassName("row justify-content-center")
                    div {
                        className = ClassName("col-lg-9")
                        div {
                            className = ClassName("row mb-2")
                            featuredContest()
                            newContestsCard()
                        }

                        div {
                            className = ClassName("row mb-2")
                            div {
                                className = ClassName("col-lg-5")
                                div {
                                    className = ClassName("card flex-md-row mb-1 box-shadow")
                                    style = jso {
                                        minHeight = 7.rem
                                    }
                                }
                            }
                            div {
                                className = ClassName("col-lg-5")
                                div {
                                    className = ClassName("card flex-md-row mb-1 box-shadow")
                                    style = jso {
                                        minHeight = 7.rem
                                    }
                                }
                            }

                            proposeContest()
                        }

                        div {
                            className = ClassName("row mb-2")
                            userRating()
                            contestList()
                        }
                    }
                }
            }
        }
    }

    companion object :
        RStatics<ContestListViewProps, ContestListViewState, ContestListView, Context<RequestStatusContext>>(
        ContestListView::class
    ) {
        init {
            contextType = requestStatusContext
        }
    }
}
