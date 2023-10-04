package com.saveourtool.save.frontend.components.views.vuln

import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.utils.useWindowOpenness
import react.VFC
import react.useState
import react.dom.html.ReactHTML.div
import com.saveourtool.osv4k.*
import com.saveourtool.save.frontend.utils.buttonBuilder

val cosvSchemaView = VFC {
    val isOpen = useWindowOpenness()
    val (textInModal, setTextInModal) = useState<String>()

    displayModal(opener = isOpen) {

    }

    div {
        buttonBuilder("schema_version") {
            setTextInModal("schema_version").body
            isOpen.openWindow()
        }
    }
}