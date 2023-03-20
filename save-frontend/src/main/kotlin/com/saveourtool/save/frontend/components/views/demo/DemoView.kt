/**
 * View with demo for diktat and ktlint
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.basic.demoRunComponent
import com.saveourtool.save.frontend.externals.reactace.AceThemes
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.Languages

import csstype.*
import js.core.get
import react.*
import react.dom.html.ReactHTML.div
import react.router.useParams

private val backgroundCard = cardComponent(hasBg = true, isPaddingBottomNull = true)

/**
 * [VFC] for demo view
 */
val demoView: VFC = VFC {
    val params = useParams()
    val projectCoordinates = "${params["organizationName"]}/${params["projectName"]}"
    var configName by useState<String>()
    useRequest(arrayOf(params)) {
        configName = get(
            url = "$demoApiUrl/manager/$projectCoordinates",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<DemoDto>().configName
            }
    }

    commonDemoView {
        emptyDemoRunRequest = DemoRunRequest.empty
        demoRunEndpoint = "/$projectCoordinates/run"
        this.configName = configName
    }
}

/**
 * [VFC] for demo view (a temporary workaround for diktat)
 */
val diktatDemoView: VFC = VFC {
    commonDemoView {
        emptyDemoRunRequest = DemoRunRequest.diktatDemoRunRequest
        demoRunEndpoint = "/diktat/run"
        configName = "diktat-analysis.xml"
    }
}

private val commonDemoView: FC<DemoViewProps> = FC { props ->
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
                demoRunComponent {
                    // todo: add editor mode selector
                    this.selectedMode = Languages.KOTLIN
                    this.selectedTheme = selectedTheme
                    this.emptyDemoRunRequest = props.emptyDemoRunRequest
                    this.demoRunEndpoint = props.demoRunEndpoint
                    this.configName = props.configName
                }
            }
        }
    }
}

/**
 * [Props] for DemoView
 */
external interface DemoViewProps : Props {
    /**
     * An initial value of [DemoRunRequest]
     */
    var emptyDemoRunRequest: DemoRunRequest

    /**
     * Endpoint to run this demo
     */
    var demoRunEndpoint: String

    /**
     * Optional config name for this demo
     */
    var configName: String?
}
