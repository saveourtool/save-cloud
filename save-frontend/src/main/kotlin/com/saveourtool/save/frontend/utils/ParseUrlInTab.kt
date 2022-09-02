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
fun <T, S : HasSelectedMenu<T>> AbstractView<*, S>.urlAnalysis(menu: TabMenuBar<T>, role: Role, isOrganizationCanCreateContest: Boolean?) {
    val href = window.location.href
    val tab = if (href.contains(menu.regexForUrlClassification)) {
        href.substringAfterLast(URL_PATH_DELIMITER).let { menu.findEnumElement(it) ?: menu.defaultTab }
    } else {
        menu.defaultTab
    }
    if (state.selectedMenu != tab) {
        if (menu.isNotAvailableWithThisRole(role, tab, isOrganizationCanCreateContest)) {
            window.alert("Your role is not suitable for opening this page")
            changeUrl(menu.defaultTab, menu)
            window.location.reload()
            setState { selectedMenu = menu.defaultTab }
        } else {
            changeUrl(tab, menu)
            setState { selectedMenu = tab }
        }
    }
}

/**
 * Creates unique url address for page tabs
 *
 * @param selectedMenu
 * @param menuBar
 */
fun <T> changeUrl(selectedMenu: T, menuBar: TabMenuBar<T>) {
    window.location.href = if (selectedMenu == menuBar.defaultTab) {
        menuBar.pathDefaultTab
    } else {
        "${menuBar.longPrefixPathAllTab}/${selectedMenu.toString().lowercase()}"
    }
}
