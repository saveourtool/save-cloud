@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.vuln.component

import com.saveourtool.save.frontend.externals.fontawesome.faFile
import com.saveourtool.save.frontend.externals.i18next.useTranslation
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.validation.FrontendRoutes
import react.FC
import react.Props
import react.router.useNavigate

val uploadCosvButton: FC<UploadCosvButtonProps> = FC { props ->
    val (t) = useTranslation("vulnerability-upload")
    val navigate = useNavigate()

    if (props.isImage) {
        buttonBuilder(faFile, style = "primary", title = "Add new vulnerability from json", classes = "icon-2-5rem", isOutline = true) {
            navigate("/${FrontendRoutes.VULN_UPLOAD}")
        }
    } else {
        buttonBuilder("Upload COSV files".t(), style = "primary", isOutline = true) {
            navigate("/${FrontendRoutes.VULN_UPLOAD}")
        }
    }
}

/**
 * [Props] for [uploadCosvButton]
 */
external interface UploadCosvButtonProps : Props {
    /**
     * Callback invoked on user click
     */
    var isImage: Boolean
}
