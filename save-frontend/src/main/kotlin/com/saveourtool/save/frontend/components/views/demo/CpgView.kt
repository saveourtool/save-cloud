/**
 * View for cpg
 */

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.frontend.components.basic.builderComponent
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.utils.selectorBuilder
import csstype.ClassName
import react.VFC
import react.dom.html.ReactHTML.div
import react.useState

private val backgroundCard = cardComponent(hasBg = false, isPaddingBottomNull = true)

val cpgView = cpgView()

private fun cpgView(): VFC = VFC {
    val (selectedTheme, setSelectedTheme) = useState(AceThemes.preferredTheme)

    div {
        className = ClassName("d-flex justify-content-center mb-2")
        div {
            className = ClassName("col-12")
            backgroundCard {
                div {
                    className = ClassName("d-flex justify-content-center")
                    div {
                        className = ClassName("mt-2 col-2")
                        selectorBuilder(
                            selectedTheme.name,
                            AceThemes.values().map { it.name },
                            "custom-select",
                        ) { setSelectedTheme(AceThemes.valueOf(it.target.value)) }
                    }
                }
                builderComponent {
                    this.selectedTheme = selectedTheme
                    this.sendRunRequest = { codeLines, language ->
                        // TODO: method for run
                    }
                }
            }
        }
    }
}
