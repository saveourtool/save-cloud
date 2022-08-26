@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.url

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.benchmarks.TabMenuBar
import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.utils.URL_PATH_DELIMITER
import com.saveourtool.save.validation.FrontendRoutes

import react.State

import kotlinx.browser.window

/**
 * General interface for working with MenuBar
 */
external interface HasSelectedMenu<T> : State {
    /**
     * selected value in T Enum
     */
    var selectedMenu: T?
}

/**
 * The function of analyzing the URL of a tabbed page goes to the tab that was entered in the url, according to the role
 *
 * @param menu
 * @param role
 * @param flag
 */
fun <T, S> AbstractView<*, S>.urlAnalysis(menu: TabMenuBar<T>, role: Role, flag: Boolean?)
where S : State, S : HasSelectedMenu<T> {
    val href = window.location.href
    val tab = if (href.contains(menu.regexForUrlClassification)) {
        href.substringAfterLast(URL_PATH_DELIMITER).run { menu.findEnumElement(this) }
    } else {
        menu.defaultTab
    }
    if (state.selectedMenu != tab) {
        if (menu.isNotAvailableWithThisRole(role, tab, flag)) {
            changeUrl(menu.defaultTab, menu)
            window.alert("Your role is not suitable for opening this page")
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
fun <T> changeUrl(selectedMenu: T?, menuBar: TabMenuBar<T>) {
    selectedMenu?.let {
        window.location.href = if (selectedMenu == menuBar.defaultTab) {
            menuBar.paths.first
        } else {
            "${menuBar.paths.second}/${menuBar.convertEnumElemToString(selectedMenu).lowercase()}"
        }
    } ?: run {
        window.location.href = "#/${FrontendRoutes.NOT_FOUND.path}"
        window.location.reload()
    }
}
