package com.saveourtool.save.cosv.frontend.components.views.welcome.pagers

import com.saveourtool.save.frontend.common.externals.animations.Animation
import react.ChildrenBuilder

val allVulnerabilityWelcomePagers: List<List<WelcomePager>> = emptyList()

/**
 * common interface for all pagers on welcome view
 */
interface WelcomePager {
    /**
     * animation for the pager
     */
    val animation: Animation

    /**
     * rendering function - place your html code here
     *
     * @param childrenBuilder
     */
    fun renderPage(childrenBuilder: ChildrenBuilder)
}
