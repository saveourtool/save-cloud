/**
 * Main view for Demos
 */

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.frontend.components.basic.demo.welcome.demoList
import com.saveourtool.save.frontend.components.basic.demo.welcome.featuredDemos
import com.saveourtool.save.frontend.components.basic.demo.welcome.introductionSection
import com.saveourtool.save.frontend.utils.*
import csstype.ClassName
import react.VFC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.main

val demoMainView: VFC = VFC {
    useBackground(Style.BLUE)

    main {
        className = ClassName("main-content mt-0 ps")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("row justify-content-center")

                div {
                    className = ClassName("col-md-4")
                    div {
                        className = ClassName("mb-2")
                        introductionSection()
                    }
                    div {
                        featuredDemos()
                    }
                }
                div {
                    className = ClassName("col-lg-7 d-flex align-items-stretch")
                    demoList()
                }
            }
        }
    }
}
