package com.saveourtool.save.frontend.components.views.url

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.benchmarks.TabMenubar
import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.utils.URL_PATH_DELIMITER
import com.saveourtool.save.validation.FrontendRoutes
import kotlinx.browser.window
import react.State


/**
 * The function of analyzing the URL of a tabbed page goes to the tab that was entered in the url, according to the role
 *
 * @param menu
 * @param role
 * @param flag
 */
fun <T, S> AbstractView<*, S>.urlAnalysis(menu: TabMenubar<T>, role: Role, flag: Boolean?)
        where S : State, S : HasSelectedMenu<T> {
    val href = window.location.href
    val tab = if (href.contains(menu.regexForUrlClassification)) {
        href.substringAfterLast(URL_PATH_DELIMITER).run { menu.findEnumElements(this) }
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
 * @param menubar
 */
fun <T, S> changeUrl(selectedMenu: T?, menubar: TabMenubar<T>)
        where S : HasSelectedMenu<T> {
    selectedMenu?.let {
        window.location.href = if (selectedMenu == menubar.defaultTab) {
            menubar.paths.first
        } else {
            "${menubar.paths.second}/${menubar.returnStringOneOfElements(selectedMenu).lowercase()}"
        }
    } ?: run {
        window.location.href = "#/${FrontendRoutes.NOT_FOUND.path}"
        window.location.reload()
    }
}


/**
 * General interface for working with MenuBar
 */
external interface HasSelectedMenu<T> : State {
    /**
     * selected value in T Enum
     */
    var selectedMenu: T?
}
