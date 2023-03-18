/**
 * View with demo
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.basic.demo.demoRunComponent
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.Languages

import csstype.*
import react.*
import react.dom.html.ReactHTML.div

private val backgroundCard = cardComponent(hasBg = true, isPaddingBottomNull = true)

/**
 * Demo View
 */
val demoView: FC<DemoViewProps> = FC { props ->
    useBackground(Style.WHITE)
    val (demoDto, setDemoDto) = useState(DemoDto.empty)
    useRequest {
        get(
            url = "$demoApiUrl/manager/${props.projectCoordinates}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<DemoDto>()
            }
            .let { setDemoDto(it) }
    }

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
                demoRunComponent {
                    // todo: add editor mode selector
                    this.selectedMode = Languages.KOTLIN
                    this.selectedTheme = selectedTheme
                    this.projectCoordinates = props.projectCoordinates
                    this.configName = demoDto.configName
                    this.availableModes = demoDto.runCommands.keys.toList()
                }
            }
        }
    }
}

/**
 * [Props] for [demoView]
 */
external interface DemoViewProps : Props {
    /**
     * saveourtool [ProjectCoordinates]
     */
    var projectCoordinates: ProjectCoordinates
}
