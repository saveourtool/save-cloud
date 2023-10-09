package com.saveourtool.save.frontend.components.views.vuln

import com.saveourtool.save.frontend.components.modal.displayModalWithPreTag
import com.saveourtool.save.frontend.components.modal.loaderModalStyle
import com.saveourtool.save.frontend.components.views.vuln.utils.COSV_SCHEMA_JSON
import react.VFC
import react.useState
import react.dom.html.ReactHTML.div
import react.router.dom.Link
import react.dom.html.ReactHTML.p
import com.saveourtool.save.frontend.components.views.vuln.utils.cosvFieldsDescriptionsMap
import com.saveourtool.save.frontend.utils.*

import react.dom.html.ReactHTML
import web.cssom.ClassName


val cosvSchemaView = VFC {
    particles()
    useBackground(Style.VULN_DARK)
    val windowOpenness = useWindowOpenness()
    val (textInModal, setTextInModal) = useState<Pair<String, String>>()

    if (textInModal != null) {
        displayModalWithPreTag(
                windowOpenness.isOpen(),
                textInModal.first,
                textInModal.second,
                loaderModalStyle,
                windowOpenness.closeWindowAction()
        ) {
            buttonBuilder("Close", "secondary") {
                windowOpenness.closeWindow()
            }
        }
    }

    div {
        className = ClassName("row justify-content-center")
        div {
            className = ClassName("col-sm-4 mt-3")
            div {
                className = ClassName("card card-body mt-0 pt-0 px-0 text-gray-800 shadow justify-content-center")
                Link {
                    +"[COSV Schema 1.0]"
                    to="https://saveourtool.github.io/cosv4k/"
                }
                // FixMe: JSON.stringify not working in kotlin js
                COSV_SCHEMA_JSON.drop(1).dropLast(1).split("\n").forEach {
                    val str = it.replace("\\", "")
                    // extract key from schema
                    val key = str.takeWhile { it != ':' }.replace("\"", "").trim()

                    div {
                        className = ClassName("row")

                        // match key with description
                        val cosvKey = cosvFieldsDescriptionsMap.firstOrNull { (k, _) ->
                            k == key
                        }

                        // show schema, where each key is the button, which will show description about this key vid `onClick`
                        ReactHTML.pre {
                            cosvKey?.let { (key, value) ->
                                // hold the tabulations
                                +"${str.takeWhile { it != '\"' }}\""
                                // make from key the button
                                buttonBuilder(key.substringAfter("."), classes = "btn-sm") {
                                    setTextInModal(key to value)
                                    windowOpenness.openWindow()
                                }
                                +"\":"
                                // print the type, i.e. value
                                +str.dropWhile { it != ':' }.drop(1)
                            } ?: run {
                                // just print raw string, if no key with description matched
                                +str
                            }
                        }
                    }
                }
            }
        }

    }
}



