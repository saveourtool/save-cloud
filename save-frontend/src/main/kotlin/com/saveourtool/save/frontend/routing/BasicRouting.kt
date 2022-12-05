/**
 * All routs for the mobile version of the frontend
 */

package com.saveourtool.save.frontend.routing

import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.entities.benchmarks.BenchmarkCategoryEnum
import com.saveourtool.save.filters.TestExecutionFilters
import com.saveourtool.save.frontend.components.views.*
import com.saveourtool.save.frontend.components.views.contests.ContestGlobalRatingView
import com.saveourtool.save.frontend.components.views.contests.ContestListView
import com.saveourtool.save.frontend.components.views.contests.UserRatingTab
import com.saveourtool.save.frontend.components.views.demo.cpgView
import com.saveourtool.save.frontend.components.views.projectcollection.CollectionView
import com.saveourtool.save.frontend.components.views.welcome.WelcomeView
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.frontend.components.views.demo.diktatDemoView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsEmailMenuView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsOrganizationsMenuView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsProfileMenuView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsTokenMenuView
import com.saveourtool.save.frontend.createRoutersWithPathAndEachListItem
import com.saveourtool.save.frontend.utils.OrganizationMenuBar
import com.saveourtool.save.frontend.utils.ProjectMenuBar
import com.saveourtool.save.frontend.utils.isSuperAdmin
import com.saveourtool.save.frontend.utils.withRouter
import com.saveourtool.save.validation.FrontendRoutes.*
import js.core.get
import org.w3c.dom.url.URLSearchParams
import react.*
import react.router.Route
import react.router.Routes
import react.router.Navigate


external interface AppProps : PropsWithChildren {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?
}

val testExecutionDetailsView = testExecutionDetailsView()


/**
 * Just put a map: View -> Route URL to this list
 */
val basicRouting = FC<AppProps> { props ->
    val fallbackNode = FallbackView::class.react.create {
        bigText = "404"
        smallText = "Page not found"
        withRouterLink = false
    }

    val contestView: FC<Props> = withRouter { location, params ->
        ContestView::class.react {
            currentUserInfo = props.userInfo
            currentContestName = params["contestName"]
            this.location = location
        }
    }

    val contestExecutionView: FC<Props> = withRouter { _, params ->
        ContestExecutionView::class.react {
            currentUserInfo = props.userInfo
            contestName = params["contestName"]!!
            organizationName = params["organizationName"]!!
            projectName = params["projectName"]!!
        }
    }

    val projectView: FC<Props> = withRouter { location, params ->
        ProjectView::class.react {
            name = params["name"]!!
            owner = params["owner"]!!
            currentUserInfo = props.userInfo
            this.location = location
        }
    }

    val historyView: FC<Props> = withRouter { _, params ->
        HistoryView::class.react {
            name = params["name"]!!
            organizationName = params["owner"]!!
        }
    }

    val executionView: FC<Props> = withRouter { location, params ->
        ExecutionView::class.react {
            executionId = params["executionId"]!!
            filters = web.url.URLSearchParams(location.search).let { params ->
                TestExecutionFilters(
                    status = params.get("status")?.let { TestResultStatus.valueOf(it) },
                    fileName = params.get("fileName"),
                    testSuite = params.get("testSuite"),
                    tag = params.get("tag")
                )
            }
        }
    }

    val creationView: FC<Props> = withRouter { _, params ->
        CreationView::class.react {
            organizationName = params["owner"]
        }
    }

    val organizationView: FC<Props> = withRouter { location, params ->
        OrganizationView::class.react {
            organizationName = params["owner"]!!
            currentUserInfo = props.userInfo
            this.location = location
        }
    }

    Routes {
        listOf(
            WelcomeView::class.react.create { userInfo = props.userInfo } to "/",
            AboutUsView::class.react.create() to "/$ABOUT_US",
            diktatDemoView.create() to "/$DEMO/diktat",
            cpgView.create() to "/$DEMO/cpg",
            SandboxView::class.react.create() to "/$SANDBOX",
            awesomeBenchmarksView.create() to "/$AWESOME_BENCHMARKS",
            RegistrationView::class.react.create { userInfo = props.userInfo } to "/$REGISTRATION",
            contestGlobalRatingView.create() to "/$CONTESTS_GLOBAL_RATING",
            contestView.create() to "/$CONTESTS/:contestName",
            contestExecutionView.create() to "/$CONTESTS/:contestName/:organizationName/:projectName",
            CreationView::class.react.create() to "/$CREATE_PROJECT",
            CreateOrganizationView::class.react.create() to "/$CREATE_ORGANIZATION",
            CollectionView::class.react.create { currentUserInfo = props.userInfo } to "/$PROJECTS",
            ContestListView::class.react.create { currentUserInfo = props.userInfo } to "/$CONTESTS",
            creationView.create() to "/$CREATE_PROJECT/:owner",
            organizationView.create() to "/:owner",
            organizationView.create() to "/${OrganizationMenuBar.nameOfTheHeadUrlSection}/:owner",
            historyView.create() to "/:owner/:name/history",
            projectView.create() to "/:owner/:name",
            executionView.create() to "/:owner/:name/history/execution/:executionId",
            testExecutionDetailsView.create() to "/:owner/:name/history/execution/:executionId/details/:testSuiteName/:pluginName/*"
        ).forEach {
            Route {
                this.element = it.first
                this.path = "/${it.second}"
            }
        }

        Route {
            path = "/${props.userInfo?.name}/$SETTINGS_PROFILE"
            element = props.userInfo?.name?.let {
                UserSettingsProfileMenuView::class.react.create {
                    userName = it
                }
            } ?: fallbackNode
        }

        Route {
            path = "/${props.userInfo?.name}/$SETTINGS_EMAIL"
            element = props.userInfo?.name?.let {
                UserSettingsEmailMenuView::class.react.create {
                    userName = it
                }
            } ?: fallbackNode
        }

        Route {
            path = "/${props.userInfo?.name}/$SETTINGS_TOKEN"
            element = props.userInfo?.name?.let {
                UserSettingsTokenMenuView::class.react.create {
                    userName = it
                }
            } ?: fallbackNode
        }

        Route {
            path = "/${props.userInfo?.name}/$SETTINGS_ORGANIZATIONS"
            element = props.userInfo?.name?.let {
                UserSettingsOrganizationsMenuView::class.react.create {
                    userName = it
                }
            } ?: fallbackNode
        }

        props.userInfo?.name.run {
            Route {
                path = "/$this"
                element = Navigate.create {
                    to = "/$this/$SETTINGS_PROFILE"
                }
            }
        }

        Route {
            path = "/$MANAGE_ORGANIZATIONS"
            element = when (props.userInfo.isSuperAdmin()) {
                true -> OrganizationAdminView::class.react.create()
                else -> fallbackNode
            }
        }

        createRoutersWithPathAndEachListItem<ProjectMenuBar>(
            "/${ProjectMenuBar.nameOfTheHeadUrlSection}/:owner/:name",
            projectView
        )

        createRoutersWithPathAndEachListItem<OrganizationMenuBar>(
            "/${OrganizationMenuBar.nameOfTheHeadUrlSection}/:owner",
            organizationView
        )

        createRoutersWithPathAndEachListItem<ContestMenuBar>(
            "/$CONTESTS/:contestName",
            contestView
        )

        createRoutersWithPathAndEachListItem<BenchmarkCategoryEnum>(
            "/${BenchmarkCategoryEnum.nameOfTheHeadUrlSection}/$AWESOME_BENCHMARKS",
            awesomeBenchmarksView
        )

        createRoutersWithPathAndEachListItem<UserRatingTab>(
            "/$CONTESTS_GLOBAL_RATING",
            contestGlobalRatingView
        )

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

val awesomeBenchmarksView: FC<Props> = withRouter { location, _ ->
    AwesomeBenchmarksView::class.react {
        this.location = location
    }
}

val contestGlobalRatingView: FC<Props> = withRouter { location, _ ->
    ContestGlobalRatingView::class.react {
        organizationName = URLSearchParams(location.search).get("organizationName")
        projectName = URLSearchParams(location.search).get("projectName")
        this.location = location
    }
}

