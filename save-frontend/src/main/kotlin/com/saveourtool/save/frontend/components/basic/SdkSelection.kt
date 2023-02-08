/**
 * Component for SDK selection
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.*

import csstype.ClassName
import react.ChildrenBuilder
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.useState
import web.html.HTMLSelectElement

/**
 * Component for sdk selection
 */
val sdkSelection = sdkSelection()

/**
 * Props for SdkSelection component
 */
external interface SdkProps : PropsWithChildren {
    /**
     * Title for sdk selector
     */
    var title: String

    /**
     * The selected SDK
     */
    var selectedSdk: Sdk

    /**
     * Callback invoked when SDK is changed
     */
    var onSdkChange: (Sdk) -> Unit

    /**
     * Flag to disable sdk selection
     */
    var isDisabled: Boolean
}

private fun ChildrenBuilder.selection(
    labelValue: String,
    value: String,
    options: List<String>,
    isDisabled: Boolean,
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
        disabled = isDisabled
        onChange = {
            val target = it.target
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
            val (sdkName, setSdkName) = useState(props.selectedSdk.name)
            val (sdkVersion, setSdkVersion) = useState(props.selectedSdk.version)
            if (props.title.isNotBlank()) {
                label {
                    className =
                            ClassName("control-label col-auto justify-content-between font-weight-bold text-gray-800 mb-1 pl-0")
                    +props.title
                }
            }
            div {
                className = ClassName("card align-items-left mb-3 pt-0 pb-0")
                div {
                    className = ClassName("card-body align-items-left pb-1 pt-3")
                    div {
                        className = ClassName("row no-gutters align-items-left")
                        selection(
                            "SDK",
                            sdkName,
                            sdks,
                            isDisabled = props.isDisabled,
                        ) { element ->
                            val newSdkName = element.value
                            val newSdkVersion = newSdkName.getSdkVersions().first()
                            setSdkName(newSdkName)
                            setSdkVersion(newSdkVersion)
                            props.onSdkChange("$newSdkName:$newSdkVersion".toSdk())
                        }
                    }
                    div {
                        className = ClassName("row no-gutters align-items-left")
                        className = ClassName("d-inline")
                        selection(
                            "Version",
                            sdkVersion,
                            sdkName.getSdkVersions(),
                            isDisabled = props.isDisabled,
                        ) { element ->
                            val newSdkVersion = element.value
                            setSdkVersion(newSdkVersion)
                            props.onSdkChange("$sdkName:$newSdkVersion".toSdk())
                        }
                    }
                }
            }
        }
