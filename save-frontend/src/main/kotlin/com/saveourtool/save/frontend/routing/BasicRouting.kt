/**
 * All routs for the mobile version of the frontend
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "EXTENSION_FUNCTION_WITH_CLASS")

package com.saveourtool.save.frontend.routing

import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.filters.TestExecutionFilter
import com.saveourtool.save.frontend.components.ErrorBoundary
import com.saveourtool.save.frontend.components.basic.cookieBanner
import com.saveourtool.save.frontend.components.basic.projects.createProjectProblem
import com.saveourtool.save.frontend.components.basic.projects.projectProblem
import com.saveourtool.save.frontend.components.basic.scrollToTopButton
import com.saveourtool.save.frontend.components.footer
import com.saveourtool.save.frontend.components.requestModalHandler
import com.saveourtool.save.frontend.components.topbar.topBarComponent
import com.saveourtool.save.frontend.components.views.*
import com.saveourtool.save.frontend.components.views.agreements.cookieTermsOfUse
import com.saveourtool.save.frontend.components.views.agreements.termsOfUsageView
import com.saveourtool.save.frontend.components.views.contests.*
import com.saveourtool.save.frontend.components.views.demo.cpgView
import com.saveourtool.save.frontend.components.views.demo.demoCollectionView
import com.saveourtool.save.frontend.components.views.demo.demoView
import com.saveourtool.save.frontend.components.views.index.indexView
import com.saveourtool.save.frontend.components.views.projectcollection.CollectionView
import com.saveourtool.save.frontend.components.views.toprating.topRatingView
import com.saveourtool.save.frontend.components.views.userprofile.userProfileView
import com.saveourtool.save.frontend.components.views.usersettings.*
import com.saveourtool.save.frontend.components.views.vuln.*
import com.saveourtool.save.frontend.components.views.welcome.saveWelcomeView
import com.saveourtool.save.frontend.components.views.welcome.vulnWelcomeView
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.isSuperAdmin
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes.*
import js.core.jso
import kotlinx.browser.window

import org.w3c.dom.url.URLSearchParams
import react.*
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.router.*
import react.router.dom.createBrowserRouter
import web.cssom.ClassName

/**
 * Just put a map: View -> Route URL to this list
 */
val basicRouting: FC<UserInfoAwareMutablePropsWithChildren> = FC { props ->
    useUserStatusRedirects(props.userInfo?.status)
    createBasicRoutes(props.userInfo, props.userInfoSetter)
        .let {
            RouterProvider {
                router = createBrowserRouter(
                    routes = it,
                    opts = jso {
                        basename = "/"
                    }
                )
            }
        }
}

private val fallbackNode = FallbackView::class.react.wrapAndCreate {
    bigText = "404"
    smallText = "Page not found"
    withRouterLink = false
}

/**
 * @param view
 * @return a view or a fallback of user info is null
 */
fun UserInfoAwareMutablePropsWithChildren.viewWithFallBack(view: ReactElement<*>) = this.userInfo?.name?.let {
    view
} ?: fallbackNode

/**
 * @param userInfo currently logged-in user or null
 * @param userInfoSetter setter of user info (it can be updated in settings on several views)
 * @return
 */
fun createBasicRoutes(
    userInfo: UserInfo?,
    userInfoSetter: StateSetter<UserInfo?>,
): Array<RouteObject> {
    val userProfileView = withRouter { _, params ->
        userProfileView {
            userName = params["name"]!!
            currentUserInfo = userInfo
        }
    }

    val contestView = withRouter { location, params ->
        ContestView::class.react {
            currentUserInfo = userInfo
            currentContestName = params["contestName"]
            this.location = location
        }
    }

    val contestExecutionView = withRouter { _, params ->
        ContestExecutionView::class.react {
            currentUserInfo = userInfo
            contestName = params["contestName"]!!
            organizationName = params["organizationName"]!!
            projectName = params["projectName"]!!
        }
    }

    val projectView = withRouter { location, params ->
        ProjectView::class.react {
            name = params["name"]!!
            owner = params["owner"]!!
            currentUserInfo = userInfo
            this.location = location
        }
    }

    val historyView = withRouter { _, params ->
        HistoryView::class.react {
            name = params["name"]!!
            organizationName = params["owner"]!!
        }
    }

    val executionView = withRouter { location, params ->
        ExecutionView::class.react {
            organization = params["organization"]!!
            project = params["project"]!!
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

    val organizationView = withRouter { location, params ->
        OrganizationView::class.react {
            organizationName = params["owner"]!!
            currentUserInfo = userInfo
            this.location = location
        }
    }

    val awesomeBenchmarksView = withRouter { location, _ ->
        AwesomeBenchmarksView::class.react {
            this.location = location
        }
    }

    val contestGlobalRatingView = withRouter { location, _ ->
        ContestGlobalRatingView::class.react {
            organizationName = URLSearchParams(location.search).get("organizationName")
            projectName = URLSearchParams(location.search).get("projectName")
            this.location = location
        }
    }

    val contestTemplateView = withRouter { _, params ->
        contestTemplateView {
            id = requireNotNull(params["id"]).toLong()
            currentUserInfo = userInfo
        }
    }

    val demoView = withRouter { _, params ->
        demoView {
            projectCoordinates = ProjectCoordinates(
                requireNotNull(params["organizationName"]),
                requireNotNull(params["projectName"]),
            )
        }
    }

    val vulnerabilityCollectionView = withRouter { location, _ ->
        vulnerabilityCollectionView {
            currentUserInfo = userInfo
            filter = URLSearchParams(location.search).toVulnerabilitiesFilter()
        }
    }

    val vulnerabilityView = withRouter { _, params ->
        vulnerabilityView {
            identifier = requireNotNull(params["identifier"])
            currentUserInfo = userInfo
        }
    }

    val createProjectProblemView = withRouter { _, params ->
        createProjectProblem {
            organizationName = requireNotNull(params["owner"])
            projectName = requireNotNull(params["name"])
        }
    }

    val projectProblemView = withRouter { _, params ->
        projectProblem {
            organizationName = requireNotNull(params["owner"])
            projectName = requireNotNull(params["name"])
            projectProblemId = requireNotNull(params["id"]).toLong()
        }
    }

    val routeToUserProfileView: RouteObject? = userInfo?.name?.let { userName ->
        jso {
            path = "/$userName"
            element = Navigate.create {
                to = "/$this/$SETTINGS_PROFILE"
            }
        }
    }

    val routeToManageOrganizationView: RouteObject = jso {
        path = "/$MANAGE_ORGANIZATIONS"
        element = when (userInfo.isSuperAdmin()) {
            true -> OrganizationAdminView::class.react.wrapAndCreate()
            else -> fallbackNode
        }
    }

    val routeToFallbackView: RouteObject = jso {
        path = "*"
        element = FallbackView::class.react.wrapAndCreate {
            bigText = "404"
            smallText = "Page not found"
            withRouterLink = true
        }
    }

    return listOf(
        indexView.wrapAndCreate { this.userInfo = userInfo } to "/",
        saveWelcomeView.wrapAndCreate { this.userInfo = userInfo } to SAVE,
        vulnWelcomeView.wrapAndCreate { this.userInfo = userInfo } to VULN,
        sandboxView.wrapAndCreate() to SANDBOX,
        AboutUsView::class.react.wrapAndCreate() to ABOUT_US,
        createOrganizationView.wrapAndCreate() to CREATE_ORGANIZATION,
        registrationView.wrapAndCreate {
            this.userInfo = userInfo
            this.userInfoSetter = userInfoSetter
        } to REGISTRATION,
        CollectionView::class.react.wrapAndCreate { currentUserInfo = userInfo } to PROJECTS,
        contestListView.wrapAndCreate { currentUserInfo = userInfo } to CONTESTS,

        FallbackView::class.react.wrapAndCreate {
            bigText = "404"
            smallText = "Page not found"
            withRouterLink = true
        } to ERROR_404,
        banView.wrapAndCreate { this.userInfo = userInfo } to BAN,
        contestGlobalRatingView.wrapAndCreate() to CONTESTS_GLOBAL_RATING,
        contestView.wrapAndCreate() to "$CONTESTS/:contestName",
        createContestTemplateView.wrapAndCreate() to CREATE_CONTESTS_TEMPLATE,
        contestTemplateView.wrapAndCreate() to "$CONTESTS_TEMPLATE/:id",
        contestExecutionView.wrapAndCreate() to "$CONTESTS/:contestName/:organizationName/:projectName",
        awesomeBenchmarksView.wrapAndCreate() to AWESOME_BENCHMARKS,
        createProjectView.wrapAndCreate() to "$CREATE_PROJECT/:organization?",
        organizationView.wrapAndCreate() to ":owner",
        historyView.wrapAndCreate() to ":owner/:name/history",
        projectView.wrapAndCreate() to ":owner/:name",
        createProjectProblemView.wrapAndCreate() to "project/:owner/:name/security/problems/new",
        projectProblemView.wrapAndCreate() to "project/:owner/:name/security/problems/:id",
        executionView.wrapAndCreate() to ":organization/:project/history/execution/:executionId",
        demoView.wrapAndCreate() to "$DEMO/:organizationName/:projectName",
        cpgView.wrapAndCreate() to "$DEMO/cpg",
        testExecutionDetailsView.wrapAndCreate() to "/:organization/:project/history/execution/:executionId/test/:testId",
        vulnerabilityCollectionView.wrapAndCreate() to "$VULN/list/:params?",
        createVulnerabilityView.wrapAndCreate() to VULN_CREATE,
        uploadVulnerabilityView.wrapAndCreate() to VULN_UPLOAD,
        vulnerabilityView.wrapAndCreate() to "$VULNERABILITY_SINGLE/:identifier",
        demoCollectionView.wrapAndCreate() to DEMO,
        userProfileView.wrapAndCreate() to "$VULN_PROFILE/:name",
        topRatingView.wrapAndCreate() to VULN_TOP_RATING,
        termsOfUsageView.wrapAndCreate() to TERMS_OF_USE,
        cookieTermsOfUse.wrapAndCreate() to COOKIE,
        thanksForRegistrationView.wrapAndCreate() to THANKS_FOR_REGISTRATION,
        cosvSchemaView.wrapAndCreate() to VULN_COSV_SCHEMA,

        userSettingsView.wrapAndCreate {
            this.userInfo = userInfo
            this.userInfoSetter = userInfoSetter
            type = SETTINGS_PROFILE
        } to SETTINGS_PROFILE,

        userSettingsView.wrapAndCreate {
            this.userInfo = userInfo
            this.userInfoSetter = userInfoSetter
            type = SETTINGS_EMAIL
        } to SETTINGS_EMAIL,

        userSettingsView.wrapAndCreate {
            this.userInfo = userInfo
            type = SETTINGS_TOKEN
        } to SETTINGS_TOKEN,

        userSettingsView.wrapAndCreate {
            this.userInfo = userInfo
            type = SETTINGS_ORGANIZATIONS
        } to SETTINGS_ORGANIZATIONS,

        userSettingsView.wrapAndCreate {
            this.userInfo = userInfo
            type = SETTINGS_DELETE
        } to SETTINGS_DELETE,

    )
        .map { (view, route) ->
            jso<RouteObject> {
                path = "/$route"
                element = view
            }
        }
        .toTypedArray()
        .let { routes ->
            routeToUserProfileView?.let { routes + it } ?: routes
        }
        .plus(routeToManageOrganizationView)
        .plus(routeToFallbackView)
}

/**
 * @param view
 * @param block
 * @return [ReactElement] created from [view] with wrapping for common parts of all pages
 */
private fun <P : UserInfoAwareProps> ElementType<P>.wrapAndCreate(
    block: (@ReactDsl P.() -> Unit)? = null,
): ReactElement<P> {
    return block?.let { this@wrapAndCreate.create(block) } ?: run { this@wrapAndCreate.create() }
//    val wrapped: FC<P> = FC { props ->
//        requestModalHandler {
//            this.userInfo = props.userInfo
//            div {
//                className = ClassName("d-flex flex-column")
//                id = "content-wrapper"
//
//                ErrorBoundary::class.react {
//                    topBarComponent { this.userInfo = props.userInfo }
//                    div {
//                        className = ClassName("container-fluid")
//                        id = "common-save-container"
//                        block?.let { this@wrapAndCreate(block) } ?: run { this@wrapAndCreate() }
//                    }
//                    if (window.location.pathname != "/$COOKIE") {
//                        cookieBanner { }
//                    }
//                    footer { }
//                }
//            }
//        }
//        scrollToTopButton()
//    }
//    return block?.let { wrapped.create(block) } ?: run { wrapped.create() }
}