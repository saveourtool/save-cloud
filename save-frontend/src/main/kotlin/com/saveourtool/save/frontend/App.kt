/**
 * Main entrypoint for SAVE frontend
 */

package com.saveourtool.save.frontend

import com.saveourtool.save.*
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.entities.benchmarks.BenchmarkCategoryEnum
import com.saveourtool.save.filters.TestExecutionFilters
import com.saveourtool.save.frontend.components.*
import com.saveourtool.save.frontend.components.basic.scrollToTopButton
import com.saveourtool.save.frontend.components.views.*
import com.saveourtool.save.frontend.components.views.contests.ContestGlobalRatingView
import com.saveourtool.save.frontend.components.views.contests.ContestListView
import com.saveourtool.save.frontend.components.views.contests.UserRatingTab
import com.saveourtool.save.frontend.components.views.projectcollection.CollectionView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsEmailMenuView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsOrganizationsMenuView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsProfileMenuView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsTokenMenuView
import com.saveourtool.save.frontend.components.views.welcome.WelcomeView
import com.saveourtool.save.frontend.externals.modal.ReactModal
import com.saveourtool.save.frontend.http.getUser
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import csstype.ClassName
import dom.html.HTMLElement
import react.*
import react.dom.client.createRoot
import react.dom.html.ReactHTML.div
import react.router.Navigate
import react.router.Route
import react.router.Routes
import react.router.dom.HashRouter
import web.url.URLSearchParams

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.js.get
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

internal val topBarComponent = topBar()

private val testExecutionDetailsView = testExecutionDetailsView()

/**
 * Top-level state of the whole App
 */
external interface AppState : State {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?
}

/**
 * Main component for the whole App
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class App : ComponentWithScope<PropsWithChildren, AppState>() {
    private val projectView: FC<Props> = withRouter { location, params ->
        ProjectView::class.react {
            name = params["name"]!!
            owner = params["owner"]!!
            currentUserInfo = state.userInfo
            this.location = location
        }
    }
    private val historyView: FC<Props> = withRouter { _, params ->
        HistoryView::class.react {
            name = params["name"]!!
            organizationName = params["owner"]!!
        }
    }
    private val executionView: FC<Props> = withRouter { location, params ->
        ExecutionView::class.react {
            executionId = params["executionId"]!!
            filters = URLSearchParams(location.search).let { params ->
                TestExecutionFilters(
                    status = params.get("status")?.let { TestResultStatus.valueOf(it) },
                    fileName = params.get("fileName"),
                    testSuite = params.get("testSuite"),
                    tag = params.get("tag")
                )
            }
        }
    }
    private val contestGlobalRatingView: FC<Props> = withRouter { location, _ ->
        ContestGlobalRatingView::class.react {
            organizationName = URLSearchParams(location.search).get("organizationName")
            projectName = URLSearchParams(location.search).get("projectName")
            this.location = location
        }
    }
    private val contestView: FC<Props> = withRouter { location, params ->
        ContestView::class.react {
            currentUserInfo = state.userInfo
            currentContestName = params["contestName"]
            this.location = location
        }
    }
    private val contestExecutionView: FC<Props> = withRouter { _, params ->
        ContestExecutionView::class.react {
            currentUserInfo = state.userInfo
            contestName = params["contestName"]!!
            organizationName = params["organizationName"]!!
            projectName = params["projectName"]!!
        }
    }
    private val creationView: FC<Props> = withRouter { location, params ->
        CreationView::class.react {
            organizationName = params["owner"]
        }
    }
    private val organizationView: FC<Props> = withRouter { location, params ->
        OrganizationView::class.react {
            organizationName = params["owner"]!!
            currentUserInfo = state.userInfo
            this.location = location
        }
    }
    private val awesomeBenchmarksView: FC<Props> = withRouter { location, _ ->
        AwesomeBenchmarksView::class.react {
            this.location = location
        }
    }
    private val fallbackNode = FallbackView::class.react.create {
        bigText = "404"
        smallText = "Page not found"
        withRouterLink = false
    }

    init {
        state.userInfo = null
    }

    override fun componentDidMount() {
        getUser()
    }

    @Suppress("TOO_LONG_FUNCTION", "NULLABLE_PROPERTY_TYPE")
    private fun getUser() {
        scope.launch {
            val userName: String? = get(
                "${window.location.origin}/sec/user",
                jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler
            ).run {
                val responseText = text().await()
                if (!ok || responseText == "null") null else responseText
            }

            val globalRole: Role? = get(
                "${window.location.origin}/api/$v1/users/global-role",
                jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler
            ).run {
                val responseText = text().await()
                if (!ok || responseText == "null") null else Json.decodeFromString(responseText)
            }

            val user: UserInfo? = userName
                ?.let { getUser(it) }

            val userInfoNew: UserInfo? = user?.copy(globalRole = globalRole) ?: userName?.let { UserInfo(name = userName, globalRole = globalRole) }

            userInfoNew?.let {
                setState {
                    userInfo = userInfoNew
                }
            }
        }
    }

    @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR", "TOO_LONG_FUNCTION", "LongMethod")
    override fun ChildrenBuilder.render() {
        HashRouter {
            requestModalHandler {
                userInfo = state.userInfo

                withRouter<Props> { location, _ ->
                    if (state.userInfo?.isActive == false && !location.pathname.startsWith("/${FrontendRoutes.REGISTRATION.path}")) {
                        Navigate {
                            to = "/${FrontendRoutes.REGISTRATION.path}"
                            replace = false
                        }
                    } else if (state.userInfo?.isActive == true && location.pathname.startsWith("/${FrontendRoutes.REGISTRATION.path}")) {
                        Navigate {
                            to = "/${FrontendRoutes.PROJECTS.path}"
                            replace = false
                        }
                    }
                }()

                div {
                    className = ClassName("d-flex flex-column")
                    id = "content-wrapper"
                    ErrorBoundary::class.react {
                        topBarComponent {
                            userInfo = state.userInfo
                        }

                        div {
                            className = ClassName("container-fluid")
                            id = "common-save-container"
                            Routes {
                                Route {
                                    path = "/"
                                    element = WelcomeView::class.react.create {
                                        userInfo = state.userInfo
                                    }
                                }

                                Route {
                                    path = "/${FrontendRoutes.SANDBOX.path}"
                                    element = SandboxView::class.react.create()
                                }

                                Route {
                                    path = "/${FrontendRoutes.AWESOME_BENCHMARKS.path}"
                                    element = awesomeBenchmarksView.create()
                                }

                                createRoutersWithPathAndEachListItem<BenchmarkCategoryEnum>(
                                    "/${BenchmarkCategoryEnum.nameOfTheHeadUrlSection}/${FrontendRoutes.AWESOME_BENCHMARKS.path}", awesomeBenchmarksView
                                )

                                Route {
                                    path = "/${FrontendRoutes.REGISTRATION.path}"
                                    element = RegistrationView::class.react.create() {
                                        userInfo = state.userInfo
                                    }
                                }

                                Route {
                                    path = "/${FrontendRoutes.CONTESTS_GLOBAL_RATING.path}"
                                    element = contestGlobalRatingView.create()
                                }

                                createRoutersWithPathAndEachListItem<UserRatingTab>("/${FrontendRoutes.CONTESTS_GLOBAL_RATING.path}", contestGlobalRatingView)

                                Route {
                                    path = "/${FrontendRoutes.CONTESTS.path}/:contestName"
                                    element = contestView.create()
                                }

                                createRoutersWithPathAndEachListItem<ContestMenuBar>("/${FrontendRoutes.CONTESTS.path}/:contestName", contestView)

                                Route {
                                    path = "/${FrontendRoutes.CONTESTS.path}/:contestName/:organizationName/:projectName"
                                    element = contestExecutionView.create()
                                }

                                Route {
                                    path = "/${FrontendRoutes.CONTESTS.path}/:contestName/:organizationName"
                                    element = Navigate.create { to = "/${FrontendRoutes.CONTESTS.path}" }
                                }

                                Route {
                                    path = "/${state.userInfo?.name}/${FrontendRoutes.SETTINGS_PROFILE.path}"
                                    element = state.userInfo?.name?.let {
                                        UserSettingsProfileMenuView::class.react.create {
                                            userName = it
                                        }
                                    } ?: fallbackNode
                                }

                                Route {
                                    path = "/${state.userInfo?.name}/${FrontendRoutes.SETTINGS_EMAIL.path}"
                                    element = state.userInfo?.name?.let {
                                        UserSettingsEmailMenuView::class.react.create {
                                            userName = it
                                        }
                                    } ?: fallbackNode
                                }

                                Route {
                                    path = "/${state.userInfo?.name}/${FrontendRoutes.SETTINGS_TOKEN.path}"
                                    element = state.userInfo?.name?.let {
                                        UserSettingsTokenMenuView::class.react.create {
                                            userName = it
                                        }
                                    } ?: fallbackNode
                                }

                                Route {
                                    path = "/${state.userInfo?.name}/${FrontendRoutes.SETTINGS_ORGANIZATIONS.path}"
                                    element = state.userInfo?.name?.let {
                                        UserSettingsOrganizationsMenuView::class.react.create {
                                            userName = it
                                        }
                                    } ?: fallbackNode
                                }

                                state.userInfo?.name.run {
                                    Route {
                                        path = "/$this"
                                        element = Navigate.create { to = "/$this/${FrontendRoutes.SETTINGS_PROFILE.path}" }
                                    }
                                }

                                Route {
                                    path = "/${FrontendRoutes.CREATE_PROJECT.path}"
                                    element = CreationView::class.react.create()
                                }

                                Route {
                                    path = "/${FrontendRoutes.CREATE_PROJECT.path}/:owner"
                                    element = creationView.create()
                                }

                                Route {
                                    path = "/${FrontendRoutes.CREATE_ORGANIZATION.path}"
                                    element = CreateOrganizationView::class.react.create()
                                }

                                Route {
                                    path = "/${FrontendRoutes.PROJECTS.path}"
                                    element = CollectionView::class.react.create {
                                        currentUserInfo = state.userInfo
                                    }
                                }

                                Route {
                                    path = "/${FrontendRoutes.CONTESTS.path}"
                                    element = ContestListView::class.react.create {
                                        currentUserInfo = state.userInfo
                                    }
                                }

                                Route {
                                    path = "/:owner"
                                    element = organizationView.create()
                                }

                                createRoutersWithPathAndEachListItem<OrganizationMenuBar>("/${OrganizationMenuBar.nameOfTheHeadUrlSection}/:owner", organizationView)

                                Route {
                                    path = "/:owner/:name/history"
                                    element = historyView.create()
                                }

                                Route {
                                    path = "/:owner/:name"
                                    element = projectView.create()
                                }

                                createRoutersWithPathAndEachListItem<ProjectMenuBar>("/${ProjectMenuBar.nameOfTheHeadUrlSection}/:owner/:name", projectView)

                                Route {
                                    path = "/:owner/:name/history/execution/:executionId"
                                    element = executionView.create()
                                }

                                Route {
                                    // Since testFilePath can represent the nested path, we catch it as *
                                    path = "/:owner/:name/history/execution/:executionId/details/:testSuiteName/:pluginName/*"
                                    element = testExecutionDetailsView.create()
                                }

                                Route {
                                    path = "*"
                                    element = FallbackView::class.react.create {
                                        bigText = "404"
                                        smallText = "Page not found"
                                        withRouterLink = true
                                    }
                                }
                            }
                        }
                        Footer::class.react()
                    }
                }
            }
            scrollToTopButton()
        }
    }
}

/**
 * The function creates routers with the given [basePath] and ending of string with all the elements given Enum<T>
 *
 * @param basePath
 * @param routeElement
 */
inline fun <reified T : Enum<T>>ChildrenBuilder.createRoutersWithPathAndEachListItem(basePath: String, routeElement: FC<Props>) {
    enumValues<T>().map { it.name.lowercase() }.forEach { item ->
        Route {
            path = "$basePath/$item"
            element = routeElement.create()
        }
    }
    Route {
        path = basePath
        element = routeElement.create()
    }
}

@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun main() {
    /* Workaround for issue: https://youtrack.jetbrains.com/issue/KT-31888 */
    if (window.asDynamic().__karma__) {
        return
    }

    kotlinext.js.require("../scss/save-frontend.scss")  // this is needed for webpack to include resource
    kotlinext.js.require("bootstrap")  // this is needed for webpack to include bootstrap
    ReactModal.setAppElement(document.getElementById("wrapper") as HTMLElement)  // required for accessibility in react-modal

    val mainDiv = document.getElementById("wrapper") as HTMLElement
    createRoot(mainDiv).render(App::class.react.create())
}
