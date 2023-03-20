/**
 * Sdk selector of projectDemoMenu
 */

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.frontend.components.basic.sdkSelection
import react.ChildrenBuilder
import react.StateSetter
import react.dom.html.ReactHTML.div

/**
 * Display [sdkSelection] of projectDemoMenu
 *
 * @param demoDto currently configured [DemoDto]
 * @param setDemoDto callback to update [demoDto] state
 * @param isDisabled flag that defines if sdk selectors should be disabled or not
 */
internal fun ChildrenBuilder.renderSdkSelector(demoDto: DemoDto, setDemoDto: StateSetter<DemoDto>, isDisabled: Boolean) {
    div {
        sdkSelection {
            title = ""
            this.isDisabled = isDisabled
            selectedSdk = demoDto.sdk
            onSdkChange = { newSdk ->
                setDemoDto { oldDemoDto ->
                    oldDemoDto.copy(sdk = newSdk)
                }
            }
        }
    }
}
