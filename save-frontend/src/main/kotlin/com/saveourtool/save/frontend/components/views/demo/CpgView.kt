/**
 * View for cpg
 */

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.basic.cpgDemoComponent

import csstype.ClassName
import react.VFC
import react.dom.html.ReactHTML.div

private val backgroundCard = cardComponent(hasBg = false, isPaddingBottomNull = true)

val cpgView = cpgView()

private fun cpgView(): VFC = VFC {
    div {
        className = ClassName("d-flex justify-content-center mb-2")
        div {
            className = ClassName("col-12")
            backgroundCard {
                cpgDemoComponent {
                    this.builderModal = { builder ->
                        // TODO: need to added window for graph
                    }
                }
            }
        }
    }
}
