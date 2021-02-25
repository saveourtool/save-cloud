package org.cqfn.save.frontend

import react.dom.render
import react.router.dom.hashRouter

import kotlinx.browser.document
import org.cqfn.save.frontend.components.FallbackView
import org.cqfn.save.frontend.components.Footer
import org.cqfn.save.frontend.components.TopBar
import react.dom.div
import react.router.dom.route

fun main() {
    kotlinext.js.require("../scss/save-frontend.scss")  // this is needed for webpack to include resource
    render(document.getElementById("wrapper")) {
        hashRouter {
            div("d-flex flex-column") {
                child(TopBar::class) {}
                div("container-fluid") {
                    route("*", FallbackView::class)
                }
                child(Footer::class) {}
            }
        }
    }
}
