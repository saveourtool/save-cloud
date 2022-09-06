@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.TabMenuBar
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.contests.contestInfoMenu
import com.saveourtool.save.frontend.components.basic.contests.contestSubmissionsMenu
import com.saveourtool.save.frontend.components.basic.contests.contestSummaryMenu
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.utils.HasSelectedMenu
import com.saveourtool.save.frontend.utils.changeUrl
import com.saveourtool.save.frontend.utils.urlAnalysis
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import react.*

import react.dom.html.InputType
import react.dom.html.ReactHTML

import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.p

import kotlinx.coroutines.launch

/**
 * Enum that defines the bar that is chosen
 */
enum class ContestMenuBar {
    INFO,
    SUBMISSIONS,
    SUMMARY,
    ;

    companion object : TabMenuBar<ContestMenuBar> {
        // The string is the postfix of a [regexForUrlClassification] for parsing the url
        private val postfixInRegex = values().map { it.name.lowercase() }.joinToString { "|" }
        override val defaultTab: ContestMenuBar = INFO
        override val regexForUrlClassification = Regex("/contests/[^/]+/[^/]+/($postfixInRegex)")
        override fun valueOf(elem: String): ContestMenuBar = ContestMenuBar.valueOf(elem)
        override fun values(): Array<ContestMenuBar> = ContestMenuBar.values()
        override fun isNotAvailableWithThisRole(role: Role, elem: ContestMenuBar?, isOrganizationCanCreateContest: Boolean?): Boolean = false
    }
}

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ContestViewProps : Props {
    var currentUserInfo: UserInfo?
    var currentContestName: String?
}

/**
 * [State] for [ContestView]
 */
external interface ContestViewState : State, HasSelectedMenu<ContestMenuBar> {
    /**
     * Flag that shows if current contest is featured or not
     */
    var isFeatured: Boolean
}


/**
 * A view with collection of projects
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ContestView : AbstractView<ContestViewProps, ContestViewState>(false) {
    init {
        state.selectedMenu = ContestMenuBar.defaultTab
        state.isFeatured = false
    }

    override fun componentDidUpdate(prevProps: ContestViewProps, prevState: ContestViewState, snapshot: Any) {
        console.log("component Did Update")
        scope.launch {
            if (state.selectedMenu != prevState.selectedMenu)
                changeUrl(state.selectedMenu, ContestMenuBar, "#/${FrontendRoutes.CONTESTS.path}/${props.currentContestName}", "#/contests/${FrontendRoutes.CONTESTS.path}/${props.currentContestName}")
            else
                urlAnalysis(ContestMenuBar, Role.NONE, false)
        }
    }

    override fun componentDidMount() {
        super.componentDidMount()
        urlAnalysis(ContestMenuBar, Role.NONE, false)
        getIsFeaturedAndSetState()
    }

    override fun ChildrenBuilder.render() {
        div {
            className = ClassName("d-flex justify-content-around")
            h1 {
                +"${props.currentContestName}"
            }
        }
        renderFeaturedCheckbox()
        renderContestMenuBar()
        when (state.selectedMenu) {
            ContestMenuBar.INFO -> renderInfo()
            ContestMenuBar.SUBMISSIONS -> renderSubmissions()
            ContestMenuBar.SUMMARY -> renderSummary()
        }
    }

    private fun ChildrenBuilder.renderFeaturedCheckbox() {
        if (props.currentUserInfo?.globalRole == Role.SUPER_ADMIN) {
            div {
                className = ClassName("d-sm-flex justify-content-center form-check pb-2")
                div {
                    input {
                        className = ClassName("form-check-input")
                        type = InputType.checkbox
                        id = "isFeaturedCheckbox"
                        checked = state.isFeatured
                        onChange = {
                            props.currentContestName?.let { contestName ->
                                addOrDeleteFeaturedContest(contestName)
                            }
                        }
                    }
                }
                div {
                    ReactHTML.label {
                        className = ClassName("form-check-label")
                        htmlFor = "isFeaturedCheckbox"
                        +"Featured contest"
                    }
                }
            }
        }
    }

    private fun ChildrenBuilder.renderSubmissions() {
        contestSubmissionsMenu {
            contestName = props.currentContestName ?: "UNDEFINED"
        }
    }

    private fun ChildrenBuilder.renderSummary() {
        contestSummaryMenu {
            contestName = props.currentContestName ?: "UNDEFINED"
        }
    }

    private fun ChildrenBuilder.renderInfo() {
        contestInfoMenu {
            contestName = props.currentContestName ?: "UNDEFINED"
        }
    }

    private fun ChildrenBuilder.renderContestMenuBar() {
        div {
            className = ClassName("row align-items-center justify-content-center")
            nav {
                className = ClassName("nav nav-tabs mb-4")
                ContestMenuBar.values()
                    .forEach { contestMenu ->
                        li {
                            className = ClassName("nav-item")
                            val classVal = if (state.selectedMenu == contestMenu) " active font-weight-bold" else ""
                            p {
                                className = ClassName("nav-link $classVal text-gray-800")
                                onClick = {
                                    if (state.selectedMenu != contestMenu) {
                                        setState { selectedMenu = contestMenu }
                                    }
                                }
                                +contestMenu.name
                            }
                        }
                    }
            }
        }
    }

    private fun ComponentWithScope<*, ContestViewState>.addOrDeleteFeaturedContest(contestName: String) = scope.launch {
        val response = post(
            "$apiUrl/contests/featured/add-or-delete?contestName=$contestName",
            jsonHeaders,
            undefined,
            loadingHandler = ::classLoadingHandler,
        )
        if (response.ok) {
            setState {
                this.isFeatured = !this.isFeatured
            }
        }
    }

    private fun ComponentWithScope<ContestViewProps, ContestViewState>.getIsFeaturedAndSetState() = scope.launch {
        val isFeatured: Boolean = get(
            "$apiUrl/contests/${props.currentContestName}/is-featured",
            jsonHeaders,
            loadingHandler = ::classLoadingHandler,
        )
            .decodeFromJsonString()
        setState {
            this.isFeatured = isFeatured
        }
    }

    companion object : RStatics<ContestViewProps, ContestViewState, ContestView, Context<RequestStatusContext>>(ContestView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
