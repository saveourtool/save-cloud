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
 * @property pathDefaultTab (must not contain a "#" for the correct execution of [navigate] in the [generateLinksWithSuffix] function)
 * @property extendedViewPath
 */
data class PathsForTabs(
    val pathDefaultTab: String,
    val extendedViewPath: String
)

/**
 * The function of analyzing the URL of a tabbed page goes to the tab that was entered in the url, according to the role
 *
 * @param menu
 * @param role
 * @param isOrganizationCanCreateContest is state.organization?.canCreateContests in OrganizationView.kt
 */
fun <T : Enum<T>, S : HasSelectedMenu<T>> AbstractView<*, S>.urlAnalysis(menu: TabMenuBar<T>, role: Role, isOrganizationCanCreateContest: Boolean?) {
    val href = window.location.href.substringBefore("?")
    val tab = if (href.contains(menu.regexForUrlClassification)) {
        href.substringAfterLast(URL_PATH_DELIMITER).let { menu.valueOfOrNull(it) ?: menu.defaultTab }
    } else {
        menu.defaultTab
    }
    if (state.selectedMenu != tab) {
        if (menu.isAvailableWithThisRole(role, tab, isOrganizationCanCreateContest)) {
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
fun <T : Enum<T>>NavigateFunctionContext.generateLinksWithSuffix(pathDefaultTab: String, suffix: String) {
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
