/**
 * View with demo for diktat and ktlint
 */

package com.saveourtool.save.frontend.components.views.demo

import com.saveourtool.save.demo.DemoInfo
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

val demoView = demoView()

private fun demoView(): VFC = VFC {
    useBackground(Style.WHITE)
    val (selectedTheme, setSelectedTheme) = useState(AceThemes.preferredTheme)
    val params = useParams()
    var emptyDemoRunRequest by useState(DemoRunRequest.empty)
    var demoRunEndpoint by useState("")
    val isDiktat = params["organizationName"] == null && params["projectName"] == null
    var configName by useState<String>()
    useRequest(arrayOf(params)) {
        if (isDiktat) {
            emptyDemoRunRequest = DemoRunRequest.diktatDemoRunRequest
            demoRunEndpoint = "/diktat/run"
            configName = "diktat-analysis.xml"
        } else {
            val projectCoordinates = "${params["organizationName"]}/${params["projectName"]}"
            demoRunEndpoint = "/$projectCoordinates/run"
            configName = get(
                url = "$apiUrl/demo/$projectCoordinates/config-name",
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
            )
                .unsafeMap {
                    it.decodeFromJsonString<DemoInfo>()
                        .demoDto
                        .configName
                }
        }
    }
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
                    this.selectedMode = Languages.KOTLIN
                    this.selectedTheme = selectedTheme
                    this.emptyDemoRunRequest = emptyDemoRunRequest
                    this.demoRunEndpoint = demoRunEndpoint
                    this.configName = configName
                }
            }
        }
    }
}
