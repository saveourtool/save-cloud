/**
 * Scroll-to-top button component
 */

package org.cqfn.save.frontend.components.basic

import org.w3c.dom.SMOOTH
import org.w3c.dom.ScrollBehavior
import org.w3c.dom.ScrollToOptions
import react.RProps
import react.dom.a
import react.dom.i
import react.functionalComponent
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
fun scrollToTopButton() = functionalComponent<RProps> {
    val (isVisible, setIsVisible) = useState(false)

    useEffect(emptyList()) {
        document.addEventListener("scroll", callback = {
            setIsVisible(window.pageYOffset > 100)
        })
    }

    if (isVisible) {
        a(classes = "scroll-to-top rounded") {
            attrs.onClickFunction = {
                window.scrollTo(ScrollToOptions(top = 0.0, behavior = ScrollBehavior.SMOOTH))
            }
            i("fas fa-angle-up") {}
        }
    }
}
