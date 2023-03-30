/**
 * Component for demo list browsing
 */

package com.saveourtool.save.frontend.components.basic.demo.welcome

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.frontend.utils.noopResponseHandler
import csstype.ClassName
import react.VFC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.useState

private val withBackground = cardComponent(isBordered = true, hasBg = true)

val demoList = VFC {
    val (organizationName, setOrganizationName) = useState("")
    val (projectName, setProjectName) = useState("")

    val (demos, setDemos) = useState<List<DemoDto>>(emptyList())

    useRequest {
        val fetchedDemos: List<DemoDto> = get(
            url = "$demoApiUrl/active",
            headers = jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler
        )
            .unsafeMap { response ->
                if (response.ok) {
                    response.decodeFromJsonString()
                } else {
                    emptyList()
                }
            }
        @Suppress("MAGIC_NUMBER")
        setDemos(fetchedDemos.take(3))
    }

    withBackground {
        div {
            className = ClassName("m-5")
            div {
                className = ClassName("input-group shadow")
                input {
                    className = ClassName("form-control")
                    placeholder = "Organization"
                    value = organizationName
                    onChange = { setOrganizationName(it.target.value) }
                }
                input {
                    className = ClassName("form-control")
                    placeholder = "Project"
                    value = projectName
                    onChange = { setProjectName(it.target.value) }
                }
            }
        }
    }
}
