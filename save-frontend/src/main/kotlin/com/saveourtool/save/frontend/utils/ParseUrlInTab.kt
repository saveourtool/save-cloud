@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.TabMenuBar
import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.utils.URL_PATH_DELIMITER

import react.State

import kotlinx.browser.window

/**
 * General interface for working with MenuBar
 */
external interface HasSelectedMenu<T : Enum<T>> : State {
    /**
     * selected value in T Enum
     */
    var selectedMenu: T
}

/**
 * The class is needed to store the paths of different tabs in different Views
 * @property pathDefaultTab is url path for the default tab (must not start with a "#" for the correct execution of [navigate] in the [navigateToLinkWithSuffix] function)
 * @property extendedViewPath is is the prefix of the path for the rest of the tabs
 */
@Suppress("KDOC_NO_CLASS_BODY_PROPERTIES_IN_HEADER")
class PathsForTabs(pathDefaultTab: String, extendedViewPath: String) {
    val pathDefaultTab: String
    val extendedViewPath: String
    init {
        this.pathDefaultTab = if (pathDefaultTab.startsWith("#/")) pathDefaultTab.removePrefix("#") else pathDefaultTab
        this.extendedViewPath = extendedViewPath
    }
}

/**
 * The function of analyzing the URL of a tabbed page goes to the tab that was entered in the url, according to the role
 *
 * @param menu
 * @param role
 * @param isOrganizationCanCreateContest is state.organization?.canCreateContests in OrganizationView.kt
 */
fun <T : Enum<T>, S : HasSelectedMenu<T>> AbstractView<*, S>.urlAnalysis(menu: TabMenuBar<T>, role: Role, isOrganizationCanCreateContest: Boolean?) {
    val href = window.location.href.substringBefore("?")
    val tab = if (href.contains(Regex(menu.regexForUrlClassification))) {
        href.substringAfterLast(URL_PATH_DELIMITER).let { menu.valueOfOrNull(it) ?: menu.defaultTab }
    } else {
        menu.defaultTab
    }
    if (state.selectedMenu != tab) {
        if (menu.isAvailableWithThisRole(role.name, tab, isOrganizationCanCreateContest)) {
            setState { selectedMenu = tab }
        } else {
            window.alert("Your role is not suitable for opening this page")
            window.location.reload()
            setState { selectedMenu = menu.defaultTab }
        }
    }
}

/**
 * @param pathDefaultTab
 * @param suffix
 */
fun NavigateFunctionContext.navigateToLinkWithSuffix(pathDefaultTab: String, suffix: String) {
    navigate(to = "$pathDefaultTab/$suffix")
}

/**
 * Creates unique url address for page tabs
 *
 * @param selectedMenu
 * @param menuBar
 * @param paths
 */
fun <T : Enum<T>> changeUrl(
    selectedMenu: T,
    menuBar: TabMenuBar<T>,
    paths: PathsForTabs
) {
    window.location.href = if (selectedMenu == menuBar.defaultTab) {
        "#${paths.pathDefaultTab}"
    } else {
        "${paths.extendedViewPath}/${selectedMenu.toString().lowercase()}"
    }
}
