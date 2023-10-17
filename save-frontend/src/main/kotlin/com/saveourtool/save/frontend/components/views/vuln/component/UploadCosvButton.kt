@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.vuln.component

import com.saveourtool.save.frontend.externals.fontawesome.faFile
import com.saveourtool.save.frontend.externals.i18next.useTranslation
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.frontend.utils.withNavigate
import com.saveourtool.save.validation.FrontendRoutes
import react.FC
import react.Props

val uploadCosvButton: FC<UploadCosvButtonProps> = FC { props ->
    val (t) = useTranslation("vulnerability-upload")

    withNavigate { navigateContext ->

        if (props.isImage) {
            buttonBuilder(faFile, style = "primary", title = "Add new vulnerability from json", classes = "icon-2-5rem", isOutline = true) {
                navigateContext.navigate("/${FrontendRoutes.UPLOAD_VULNERABILITY}")
            }
        } else {
            buttonBuilder("Upload COSV files".t(), style = "primary", isOutline = true, classes = "icon-2-5rem") {
                navigateContext.navigate("/${FrontendRoutes.UPLOAD_VULNERABILITY}")
            }
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
