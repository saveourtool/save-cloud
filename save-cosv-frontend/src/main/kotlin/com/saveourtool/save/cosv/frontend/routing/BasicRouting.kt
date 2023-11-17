/**
 * All routs for the mobile version of the frontend
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "EXTENSION_FUNCTION_WITH_CLASS")

package com.saveourtool.save.cosv.frontend.routing

import com.saveourtool.save.cosv.frontend.components.views.vuln.*
import com.saveourtool.save.cosv.frontend.components.views.vuln.vulnerabilityCollectionView
import com.saveourtool.save.cosv.frontend.components.views.welcome.vulnWelcomeView
import com.saveourtool.save.frontend.common.components.views.FallbackView
import com.saveourtool.save.frontend.common.utils.*
import com.saveourtool.save.frontend.common.utils.withRouter
import com.saveourtool.save.validation.FrontendRoutes.*

import org.w3c.dom.url.URLSearchParams
import react.*
import react.router.*

/**
 * Just put a map: View -> Route URL to this list
 */
val basicRouting: FC<UserInfoAwareMutablePropsWithChildren> = FC { props ->
    useUserStatusRedirects(props.userInfo?.status)

    val vulnerabilityCollectionView = withRouter { location, _ ->
        vulnerabilityCollectionView {
            currentUserInfo = props.userInfo
            filter = URLSearchParams(location.search).toVulnerabilitiesFilter()
        }
    }

    val vulnerabilityView = withRouter { _, params ->
        vulnerabilityView {
            identifier = requireNotNull(params["identifier"])
            currentUserInfo = props.userInfo
        }
    }

    Routes {
        listOf(
            vulnWelcomeView.create { userInfo = props.userInfo } to VULN,
            FallbackView::class.react.create {
                bigText = "404"
                smallText = "Page not found"
                withRouterLink = true
            } to ERROR_404,
            vulnerabilityCollectionView.create() to "$VULN/list/:params?",
            createVulnerabilityView.create() to VULN_CREATE,
            uploadVulnerabilityView.create() to VULN_UPLOAD,
            vulnerabilityView.create() to "$VULNERABILITY_SINGLE/:identifier",
            cosvSchemaView.create() to VULN_COSV_SCHEMA,

        ).forEach { (view, route) ->
            PathRoute {
                this.element = view
                this.path = "/$route"
            }
        }

        props.userInfo?.name?.run {
            PathRoute {
                path = "/$this"
                element = Navigate.create { to = "/$this/$SETTINGS_PROFILE" }
            }
        }

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
 * @param view
 * @return a view or a fallback of user info is null
 */
fun UserInfoAwareMutablePropsWithChildren.viewWithFallBack(view: ReactElement<*>) = this.userInfo?.name?.let {
    view
} ?: fallbackNode
