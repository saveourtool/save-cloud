package org.cqfn.save.frontend

import react.dom.render
import react.router.dom.hashRouter

import kotlinx.browser.document
import kotlinx.html.id
import org.cqfn.save.frontend.components.FallbackView
import org.cqfn.save.frontend.components.Footer
import org.cqfn.save.frontend.components.ProjectView
import org.cqfn.save.frontend.components.TopBar
import react.dom.div
import react.router.dom.route
import react.router.dom.switch
import react.router.dom.withRouter

fun main() {
    kotlinext.js.require("../scss/save-frontend.scss")  // this is needed for webpack to include resource
    render(document.getElementById("wrapper")) {
        hashRouter {
            div("d-flex flex-column") {
                attrs.id = "content-wrapper"
                route("*") {
                    // `withRouter` needs to be wrapped in `route`; we place it outside of `switch` to render always and unconditionally
                    withRouter(TopBar::class).invoke {
                } }
                div("container-fluid") {
                    switch {
                        route("/:type/:owner/:name") {
                            withRouter(ProjectView::class).invoke {}
                        }
                        route("*", FallbackView::class)
                    }
                }
                child(Footer::class) {}
            }
        }
    }
}
