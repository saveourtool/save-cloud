package com.saveourtool.save.frontend.components.views.vuln

import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.utils.useWindowOpenness
import react.VFC
import react.useState
import react.dom.html.ReactHTML.div
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.utils.Style
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.frontend.utils.useBackground

val cosvSchemaView = VFC {
    useBackground(Style.VULN_DARK)
    val windowOpenness = useWindowOpenness()
    val (textInModal, setTextInModal) = useState<String>()

    displayModal(
            windowOpenness.isOpen(),
            "TITLE",
            "MESSAGE",
            mediumTransparentModalStyle,
            windowOpenness.closeWindowAction()
    ) {
        buttonBuilder("Close", "secondary") {

        }
    }

    div {
        buttonBuilder("schema_version") {
            setTextInModal("schema_version")
            windowOpenness.openWindow()
        }
    }
}