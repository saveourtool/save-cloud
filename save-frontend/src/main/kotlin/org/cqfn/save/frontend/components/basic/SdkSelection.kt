/**
 * Component for SDK selection
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.domain.getSdkVersions
import org.cqfn.save.domain.sdks

import org.w3c.dom.HTMLSelectElement
import react.PropsWithChildren
import react.dom.RDOMBuilder
import react.dom.div
import react.dom.h5
import react.dom.h6
import react.dom.option
import react.dom.select
import react.fc

import kotlinx.html.Tag
import kotlinx.html.classes
import kotlinx.html.js.onChangeFunction

/**
 * Props for SdkSelection component
 */
external interface SdkProps : PropsWithChildren {
    /**
     * Name of the selected SDK
     */
    var selectedSdk: String

    /**
     * Version of the selected SDK
     */
    var selectedSdkVersion: String
}

private fun <T : Tag> RDOMBuilder<T>.selection(
    value: String,
    options: List<String>,
    onChange: (HTMLSelectElement) -> Unit,
) = div("d-inline-block ml-2") {
    select("form-select form-select-sm mb-3") {
        attrs.value = value
        attrs.onChangeFunction = {
            val target = it.target as HTMLSelectElement
            onChange(target)
        }
        options.forEach {
            option {
                attrs.value = it
                +it
            }
        }
    }
}

/**
 * @param onSdkChange invoked when `input` for SDK name is changed
 * @param onVersionChange invoked when `input` for SDK version is changed
 * @return a RComponent
 */
fun sdkSelection(onSdkChange: (HTMLSelectElement) -> Unit, onVersionChange: (HTMLSelectElement) -> Unit) = fc<SdkProps> { props ->
    div {
        div {
            div("d-inline-block") {
                h5 {
                    +"SDK:"
                }
            }
            selection(
                props.selectedSdk,
                sdks,
                onSdkChange,
            )
        }
        div {
            attrs.classes = if (props.selectedSdk == "Default") setOf("d-none") else setOf("d-inline ml-3")
            div("d-inline-block") {
                h6 {
                    +"SDK's version:"
                }
            }
            selection(
                props.selectedSdkVersion,
                props.selectedSdk.getSdkVersions(),
                onVersionChange,
            )
        }
    }
}
