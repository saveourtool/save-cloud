@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.TabMenuBar
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.contests.contestInfoMenu
import com.saveourtool.save.frontend.components.basic.contests.contestSubmissionsMenu
import com.saveourtool.save.frontend.components.basic.contests.contestSummaryMenu
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.http.getContest
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.HasSelectedMenu
import com.saveourtool.save.frontend.utils.changeUrl
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.frontend.utils.urlAnalysis
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.p
import remix.run.router.Location
import web.html.InputType

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
        private val postfixInRegex = values().joinToString("|") { it.name.lowercase() }
        override val nameOfTheHeadUrlSection = ""
        override val defaultTab: ContestMenuBar = INFO
        override val regexForUrlClassification = Regex("/${FrontendRoutes.CONTESTS.path}/[^/]+/($postfixInRegex)")
        override fun valueOf(elem: String): ContestMenuBar = ContestMenuBar.valueOf(elem)
        override fun values(): Array<ContestMenuBar> = ContestMenuBar.values()
    }
}

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ContestViewProps : Props {
    var currentUserInfo: UserInfo?
    var currentContestName: String?
    var location: Location
}

/**
 * [State] for [ContestView]
 */
external interface ContestViewState : State, HasSelectedMenu<ContestMenuBar> {
    /**
     * Flag that shows if current contest is featured or not
     */
    var isFeatured: Boolean

    /**
     * Contest. This field is acts as a marker of contest existence
     */
    var contest: ContestDto

    /**
     * Contains the paths of default and other tabs
     */
    var paths: PathsForTabs
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
        state.contest = ContestDto.empty
    }

    override fun componentDidUpdate(prevProps: ContestViewProps, prevState: ContestViewState, snapshot: Any) {
        if (state.selectedMenu != prevState.selectedMenu) {
            changeUrl(state.selectedMenu, ContestMenuBar, state.paths)
        } else if (props.location != prevProps.location) {
            urlAnalysis(ContestMenuBar, Role.NONE, false)
        } else if (props.currentContestName != prevProps.currentContestName) {
            fetchContest()
        }
    }

    override fun componentDidMount() {
        super.componentDidMount()
        setState { paths = PathsForTabs("/${FrontendRoutes.CONTESTS.path}/${props.currentContestName}", "#/${FrontendRoutes.CONTESTS.path}/${props.currentContestName}") }
        urlAnalysis(ContestMenuBar, Role.NONE, false)
        getIsFeaturedAndSetState()
        fetchContest()
    }

    private fun fetchContest() {
        scope.launch {
            val name = props.currentContestName
            name?.let {
                val contest = getContest(name)
                setState {
                    this.contest = contest
                }
            }
        }
    }

    override fun ChildrenBuilder.render() {
        div {
            className = ClassName("d-flex justify-content-around")
            h1 {
                +state.contest.name
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
        if (props.currentUserInfo.isSuperAdmin()) {
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
                    label {
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

    companion object : RStatics<ContestViewProps, ContestViewState, ContestView, Context<RequestStatusContext?>>(ContestView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
