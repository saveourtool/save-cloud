/**
 * All routs for the mobile version of the frontend
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "EXTENSION_FUNCTION_WITH_CLASS")

package com.saveourtool.save.frontend.routing

import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.entities.benchmarks.BenchmarkCategoryEnum
import com.saveourtool.save.filters.TestExecutionFilter
import com.saveourtool.save.frontend.components.basic.projects.createProjectProblem
import com.saveourtool.save.frontend.components.basic.projects.projectProblem
import com.saveourtool.save.frontend.components.views.*
import com.saveourtool.save.frontend.components.views.contests.*
import com.saveourtool.save.frontend.components.views.demo.cpgView
import com.saveourtool.save.frontend.components.views.demo.demoMainView
import com.saveourtool.save.frontend.components.views.demo.demoView
import com.saveourtool.save.frontend.components.views.fossgraph.createVulnerabilityView
import com.saveourtool.save.frontend.components.views.fossgraph.fossGraph
import com.saveourtool.save.frontend.components.views.fossgraph.fossGraphCollectionView
import com.saveourtool.save.frontend.components.views.index.indexView
import com.saveourtool.save.frontend.components.views.projectcollection.CollectionView
import com.saveourtool.save.frontend.components.views.toprating.topRatingView
import com.saveourtool.save.frontend.components.views.userprofile.userProfileView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsEmailMenuView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsOrganizationsMenuView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsProfileMenuView
import com.saveourtool.save.frontend.components.views.usersettings.UserSettingsTokenMenuView
import com.saveourtool.save.frontend.components.views.welcome.WelcomeView
import com.saveourtool.save.frontend.createRoutersWithPathAndEachListItem
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.isSuperAdmin
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes.*

import org.w3c.dom.url.URLSearchParams
import react.*
import react.router.*

val testExecutionDetailsView = testExecutionDetailsView()

/**
 * Just put a map: View -> Route URL to this list
 */
val basicRouting: FC<AppProps> = FC { props ->

    val userProfileView: VFC = withRouter { _, params ->
        userProfileView {
            userName = params["name"]!!
        }
    }

    val contestView: VFC = withRouter { location, params ->
        ContestView::class.react {
            currentUserInfo = props.userInfo
            currentContestName = params["contestName"]
            this.location = location
        }
    }

    val contestExecutionView: VFC = withRouter { _, params ->
        ContestExecutionView::class.react {
            currentUserInfo = props.userInfo
            contestName = params["contestName"]!!
            organizationName = params["organizationName"]!!
            projectName = params["projectName"]!!
        }
    }

    val projectView: VFC = withRouter { location, params ->
        ProjectView::class.react {
            name = params["name"]!!
            owner = params["owner"]!!
            currentUserInfo = props.userInfo
            this.location = location
        }
    }

    val historyView: VFC = withRouter { _, params ->
        HistoryView::class.react {
            name = params["name"]!!
            organizationName = params["owner"]!!
        }
    }

    val executionView: VFC = withRouter { location, params ->
        ExecutionView::class.react {
            executionId = params["executionId"]!!
            filters = web.url.URLSearchParams(location.search).let { params ->
                TestExecutionFilter(
                    status = params["status"]?.let { TestResultStatus.valueOf(it) },
                    fileName = params["fileName"],
                    testSuite = params["testSuite"],
                    tag = params["tag"]
                )
            }
            testAnalysisEnabled = true
        }
    }

    val creationView: VFC = withRouter { _, params ->
        CreationView::class.react {
            organizationName = params["owner"]
        }
    }

    val organizationView: VFC = withRouter { location, params ->
        OrganizationView::class.react {
            organizationName = params["owner"]!!
            currentUserInfo = props.userInfo
            this.location = location
        }
    }

    val awesomeBenchmarksView: VFC = withRouter { location, _ ->
        AwesomeBenchmarksView::class.react {
            this.location = location
        }
    }

    val contestGlobalRatingView: VFC = withRouter { location, _ ->
        ContestGlobalRatingView::class.react {
            organizationName = URLSearchParams(location.search).get("organizationName")
            projectName = URLSearchParams(location.search).get("projectName")
            this.location = location
        }
    }

    val contestTemplateView: VFC = withRouter { _, params ->
        contestTemplateView {
            id = requireNotNull(params["id"]).toLong()
            currentUserInfo = props.userInfo
        }
    }

    val demoView: VFC = withRouter { _, params ->
        demoView {
            projectCoordinates = ProjectCoordinates(
                requireNotNull(params["organizationName"]),
                requireNotNull(params["projectName"]),
            )
        }
    }

    val fossGraphCollectionView: VFC = VFC {
        fossGraphCollectionView {
            currentUserInfo = props.userInfo
        }
    }

    val fossGraphView: VFC = withRouter { _, params ->
        fossGraph {
            name = requireNotNull(params["vulnerabilityName"])
            currentUserInfo = props.userInfo
        }
    }

    val createProjectProblemView: VFC = withRouter { _, params ->
        createProjectProblem {
            organizationName = requireNotNull(params["owner"])
            projectName = requireNotNull(params["name"])
        }
    }

    val projectProblemView: VFC = withRouter { _, params ->
        projectProblem {
            organizationName = requireNotNull(params["owner"])
            projectName = requireNotNull(params["name"])
            projectProblemId = requireNotNull(params["id"]).toLong()
        }
    }

    Routes {
        listOf(
            indexView.create { userInfo = props.userInfo } to "/",
            WelcomeView::class.react.create { userInfo = props.userInfo } to "/$SAVE",
            sandboxView.create() to "/$SANDBOX",
            AboutUsView::class.react.create() to "/$ABOUT_US",
            CreationView::class.react.create() to "/$CREATE_PROJECT",
            CreateOrganizationView::class.react.create() to "/$CREATE_ORGANIZATION",
            RegistrationView::class.react.create { userInfo = props.userInfo } to "/$REGISTRATION",
            CollectionView::class.react.create { currentUserInfo = props.userInfo } to "/$PROJECTS",
            contestListView.create { currentUserInfo = props.userInfo } to "/$CONTESTS",

            contestGlobalRatingView.create() to "/$CONTESTS_GLOBAL_RATING",
            contestView.create() to "/$CONTESTS/:contestName",
            createContestTemplateView.create() to "/$CREATE_CONTESTS_TEMPLATE",
            contestTemplateView.create() to "/$CONTESTS_TEMPLATE/:id",
            contestExecutionView.create() to "/$CONTESTS/:contestName/:organizationName/:projectName",
            awesomeBenchmarksView.create() to "/$AWESOME_BENCHMARKS",
            creationView.create() to "/$CREATE_PROJECT/:owner",
            organizationView.create() to "/:owner",
            organizationView.create() to "/${OrganizationMenuBar.nameOfTheHeadUrlSection}/:owner",
            historyView.create() to "/:owner/:name/history",
            projectView.create() to "/:owner/:name",
            createProjectProblemView.create() to "project/:owner/:name/security/problems/new",
            projectProblemView.create() to "project/:owner/:name/security/problems/:id",
            executionView.create() to "/:owner/:name/history/execution/:executionId",
            demoView.create() to "/$DEMO/:organizationName/:projectName",
            cpgView.create() to "/$DEMO/cpg",
            testExecutionDetailsView.create() to "/:owner/:name/history/execution/:executionId/details/:testSuiteName/:pluginName/*",
            fossGraphCollectionView.create() to "/$VULNERABILITIES",
            createVulnerabilityView.create() to "/$CREATE_VULNERABILITY",
            fossGraphView.create() to "/$VULNERABILITIES/:vulnerabilityName",
            demoMainView.create() to "/$DEMO",
            userProfileView.create() to "/$PROFILE/:name",
            topRatingView.create() to "/$TOP_RATING",

            props.viewWithFallBack(
                UserSettingsProfileMenuView::class.react.create { userName = props.userInfo?.name }
            ) to "/${props.userInfo?.name}/$SETTINGS_PROFILE",

            props.viewWithFallBack(
                UserSettingsEmailMenuView::class.react.create { userName = props.userInfo?.name }
            ) to "/${props.userInfo?.name}/$SETTINGS_EMAIL",

            props.viewWithFallBack(
                UserSettingsTokenMenuView::class.react.create { userName = props.userInfo?.name }
            ) to "/${props.userInfo?.name}/$SETTINGS_TOKEN",

            props.viewWithFallBack(
                UserSettingsOrganizationsMenuView::class.react.create { userName = props.userInfo?.name }
            ) to "/${props.userInfo?.name}/$SETTINGS_ORGANIZATIONS",

        ).forEach {
            PathRoute {
                this.element = it.first
                this.path = "/${it.second}"
            }
        }

        props.userInfo?.name.run {
            PathRoute {
                path = "/$this"
                element = Navigate.create {
                    to = "/$this/$SETTINGS_PROFILE"
                }
            }
        }

        PathRoute {
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

        PathRoute {
            path = "*"
            element = FallbackView::class.react.create {
                bigText = "404"
                smallText = "Page not found"
                withRouterLink = true
            }
        }
    }
}

private val fallbackNode = FallbackView::class.react.create {
    bigText = "404"
    smallText = "Page not found"
    withRouterLink = false
}

/**
 * Property to propagate user info from App
 */
external interface AppProps : PropsWithChildren {
    /**
     * Currently logged-in user or null
     */
    var userInfo: UserInfo?
}

/**
 * @param view
 * @return a view or a fallback of user info is null
 */
fun AppProps.viewWithFallBack(view: ReactElement<*>) =
        this.userInfo?.name?.let {
            view
        } ?: fallbackNode
