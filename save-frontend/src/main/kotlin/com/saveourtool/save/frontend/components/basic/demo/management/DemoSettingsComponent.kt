/**
 * Demo file names input forms
 */

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.save.demo.DemoDto
import csstype.ClassName
import react.*
import react.dom.html.AutoComplete
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input

/**
 * Display demo file names input forms such as config filename form, input filename form etc
 *
 * @param demoDto currently configured [DemoDto]
 * @param setDemoDto callback to update [demoDto] state
 */
internal fun ChildrenBuilder.renderDemoSettings(demoDto: DemoDto, setDemoDto: StateSetter<DemoDto>) {
    div {
        div {
            input {
                className = ClassName("form-control col mb-2")
                autoComplete = AutoComplete.off
                placeholder = "Test file name"
                value = demoDto.fileName
                this.disabled = disabled
                onChange = { event ->
                    setDemoDto { request ->
                        request.copy(fileName = event.target.value)
                    }
                }
            }
        }
        div {
            className = ClassName("d-flex justify-content-between")
            input {
                className = ClassName("form-control col mr-1")
                autoComplete = AutoComplete.off
                placeholder = "Output file name"
                value = demoDto.outputFileName
                this.disabled = disabled
                onChange = { event ->
                    setDemoDto { request ->
                        request.copy(outputFileName = event.target.value.ifBlank { null })
                    }
                }
            }
            input {
                className = ClassName("form-control col ml-1")
                autoComplete = AutoComplete.off
                placeholder = "Config name"
                value = demoDto.configName
                this.disabled = disabled
                onChange = { event ->
                    setDemoDto { request ->
                        request.copy(configName = event.target.value.ifBlank { null })
                    }
                }
            }
        }
    }
}
