/**
 * View with demo for diktat and ktlint
 */

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.basic.diktatDemoComponent
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.utils.Style
import com.saveourtool.save.frontend.utils.selectorBuilder
import com.saveourtool.save.frontend.utils.useBackground
import com.saveourtool.save.utils.Languages

import csstype.*
import react.*
import react.dom.html.ReactHTML.div

private val backgroundCard = cardComponent(hasBg = true, isPaddingBottomNull = true)

val diktatDemoView = diktatDemoView()

private fun diktatDemoView(): VFC = VFC {
    useBackground(Style.WHITE)
    val (selectedTheme, setSelectedTheme) = useState(AceThemes.preferredTheme)
    div {
        className = ClassName("d-flex justify-content-center mb-2")
        div {
            className = ClassName("col-8")
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
                diktatDemoComponent {
                    this.selectedMode = Languages.KOTLIN
                    this.selectedTheme = selectedTheme
                }
            }
        }
    }
}
