package org.cqfn.save.frontend.components.basic

import kotlinx.html.classes
import kotlinx.html.js.onChangeFunction
import org.cqfn.save.domain.getSdkVersion
import org.cqfn.save.domain.sdks
import org.w3c.dom.HTMLSelectElement
import react.RProps
import react.dom.div
import react.dom.h5
import react.dom.h6
import react.dom.option
import react.dom.select
import react.fc

external interface SdkProps : RProps {
    var selectedSdk: String
    var selectedSdkVersion: String
}

fun sdkSelection(onSdkChange: (HTMLSelectElement) -> Unit, onVersionChange: (HTMLSelectElement) -> Unit) = fc<SdkProps> { props ->
    div {
        div {
            div("d-inline-block") {
                h5 {
                    +"SDK:"
                }
            }
            div("d-inline-block ml-2") {
                select("form-control form-control mb-3") {
                    attrs.value = props.selectedSdk
                    attrs.onChangeFunction = {
                        val target = it.target as HTMLSelectElement
                        onSdkChange(target)
                    }
                    sdks.forEach {
                        option {
                            attrs.value = it
                            +it
                        }
                    }
                }
            }
        }
        div {
            attrs.classes =
                if (props.selectedSdk == "Default") setOf("d-none") else setOf("d-inline ml-3")
            div("d-inline-block") {
                h6 {
                    +"SDK's version:"
                }
            }
            div("d-inline-block ml-2") {
                select("form-select form-select-sm mb-3") {
                    attrs.value = props.selectedSdkVersion
                    attrs.onChangeFunction = {
                        val target = it.target as HTMLSelectElement
                        onVersionChange(target)
                    }
                    props.selectedSdk.getSdkVersion().forEach {
                        option {
                            attrs.value = it
                            +it
                        }
                    }
                }
            }
        }
    }
}