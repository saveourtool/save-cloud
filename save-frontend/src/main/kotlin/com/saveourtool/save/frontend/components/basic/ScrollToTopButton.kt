/**
 * Scroll-to-top button component
 */

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.frontend.externals.fontawesome.faAngleUp
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon

import org.w3c.dom.SMOOTH
import org.w3c.dom.ScrollBehavior
import org.w3c.dom.ScrollToOptions
import react.PropsWithChildren
import react.dom.a
import react.fc
import react.useEffect
import react.useState

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.js.onClickFunction

/**
 * Renders scroll to top button
 *
 * @return a functional component
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun scrollToTopButton() = fc<PropsWithChildren> {
    val (isVisible, setIsVisible) = useState(false)

    useEffect {
        document.addEventListener("scroll", callback = {
            setIsVisible(window.pageYOffset > 100)
        })
    }

    if (isVisible) {
        a(classes = "scroll-to-top rounded") {
            attrs.onClickFunction = {
                window.scrollTo(ScrollToOptions(top = 0.0, behavior = ScrollBehavior.SMOOTH))
            }
            fontAwesomeIcon(icon = faAngleUp)
        }
    }
}
