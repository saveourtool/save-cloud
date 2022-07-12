/**
 * Component for SDK selection
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.getSdkVersions
import com.saveourtool.save.domain.sdks

import csstype.ClassName
import org.w3c.dom.HTMLSelectElement
import react.ChildrenBuilder
import react.FC
import react.PropsWithChildren
import react.dom.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select

import kotlinx.html.id

/**
 * Component for sdk selection
 */
val sdkSelection = sdkSelection()

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

    /**
     * Callback invoked when `input` for SDK name is changed
     */
    var onSdkChange: (HTMLSelectElement) -> Unit

    /**
     * Callback invoked when `input` for SDK version is changed
     */
    var onVersionChange: (HTMLSelectElement) -> Unit
}

private fun ChildrenBuilder.selection(
    labelValue: String,
    value: String,
    options: List<String>,
    onChangeFun: (HTMLSelectElement) -> Unit,
) = div {
    className = ClassName("input-group mb-3")
    div {
        className = ClassName("input-group-prepend")
        label {
            className = ClassName("input-group-text")
            +labelValue
        }
    }
    select {
        className = ClassName("custom-select")
        this.value = value
        onChange = {
            val target = it.target as HTMLSelectElement
            onChangeFun(target)
        }
        id = labelValue
        options.forEach {
            option {
                this.value = it
                +it
            }
        }
    }
}

private fun sdkSelection() =
        FC<SdkProps> { props ->
            label {
                className = ClassName("control-label col-auto justify-content-between font-weight-bold text-gray-800 mb-1 pl-0")
                +"2. Select the SDK if needed:"
            }
            div {
                className = ClassName("card align-items-left mb-3 pt-0 pb-0")
                div {
                    className = ClassName("card-body align-items-left pb-1 pt-3")
                    div {
                        className = ClassName("row no-gutters align-items-left")
                        selection(
                            "SDK",
                            props.selectedSdk,
                            sdks,
                            props.onSdkChange,
                        )
                    }
                    div {
                        className = ClassName("row no-gutters align-items-left")
                        className = ClassName("d-inline")
                        selection(
                            "Version",
                            props.selectedSdkVersion,
                            props.selectedSdk.getSdkVersions(),
                            props.onVersionChange,
                        )
                    }
                }
            }
        }
