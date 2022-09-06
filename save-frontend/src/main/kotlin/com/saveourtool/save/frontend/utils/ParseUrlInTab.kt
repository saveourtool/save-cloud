@file:Suppress("FILE_NAME_MATCH_CLASS")

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
 * The function of analyzing the URL of a tabbed page goes to the tab that was entered in the url, according to the role
 *
 * @param menu
 * @param role
 * @param isOrganizationCanCreateContest is state.organization?.canCreateContests in OrganizationView.kt
 */
fun <T : Enum<T>, S : HasSelectedMenu<T>> AbstractView<*, S>.urlAnalysis(menu: TabMenuBar<T>, role: Role, isOrganizationCanCreateContest: Boolean?) {
    val href = window.location.href
    val tab = if (href.contains(menu.regexForUrlClassification)) {
        href.substringAfterLast(URL_PATH_DELIMITER).let { menu.valueOfOrNull(it) ?: menu.defaultTab }
    } else {
        menu.defaultTab
    }
    if (state.selectedMenu != tab) {
        if (menu.isNotAvailableWithThisRole(role, tab, isOrganizationCanCreateContest)) {
            window.alert("Your role is not suitable for opening this page")
            window.location.reload()
            setState { selectedMenu = menu.defaultTab }
        } else {
            setState { selectedMenu = tab }
        }
    }
}

/**
 * Creates unique url address for page tabs
 *
 * @param selectedMenu
 * @param menuBar
 * @param pathDefaultTab
 * @param extendedViewPath
 */
fun <T : Enum<T>> changeUrl(
    selectedMenu: T,
    menuBar: TabMenuBar<T>,
    pathDefaultTab: String,
    extendedViewPath: String
) {
    window.location.href = if (selectedMenu == menuBar.defaultTab) {
        pathDefaultTab
    } else {
        "$extendedViewPath/${selectedMenu.toString().lowercase()}"
    }
}
