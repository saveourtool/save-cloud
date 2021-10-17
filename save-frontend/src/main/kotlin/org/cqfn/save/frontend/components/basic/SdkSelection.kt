/**
 * Component for SDK selection
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.domain.getSdkVersions
import org.cqfn.save.domain.sdks

import org.w3c.dom.HTMLSelectElement
import react.PropsWithChildren
import react.fc

import kotlinx.html.Tag
import kotlinx.html.classes
import kotlinx.html.form
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import react.dom.*

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
    labelValue: String,
    value: String,
    options: List<String>,
    onChange: (HTMLSelectElement) -> Unit,
) = div("input-group mb-3") {
    div("input-group-prepend") {
        label("input-group-text") {
            attrs["for"] = "inputGroupSelect01"
            +labelValue
        }
    }
    select("custom-select") {
        attrs.value = value
        attrs.onChangeFunction = {
            val target = it.target as HTMLSelectElement
            onChange(target)
        }
        attrs.id = labelValue
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
fun sdkSelection(onSdkChange: (HTMLSelectElement) -> Unit, onVersionChange: (HTMLSelectElement) -> Unit) =
    fc<SdkProps> { props ->
        div("card align-items-left mb-3 pt-0 pb-0") {
            div("card-body align-items-left pb-1 pt-3") {
                div("row no-gutters align-items-left") {
                    selection(
                        "SDK",
                        props.selectedSdk,
                        sdks,
                        onSdkChange,
                    )
                }
                div("row no-gutters align-items-left") {
                    attrs.classes = setOf("d-inline")
                    selection(
                        "Version",
                        props.selectedSdkVersion,
                        props.selectedSdk.getSdkVersions(),
                        onVersionChange,
                    )
                }
            }
        }
    }
