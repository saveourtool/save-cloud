/**
 * Authorization component (Oauth2 elements) for Index View
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.externals.i18next.useTranslation
import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import web.cssom.*

const val INDEX_VIEW_CUSTOM_BG = "rgb(247, 250, 253)"

@Suppress("IDENTIFIER_LENGTH")
val indexViewInfo: FC<IndexViewProps> = FC { props ->
    val (t) = useTranslation("index")

    div {
        className = ClassName("row justify-content-center mt-5 text-gray-900")
        h2 {
            +"SaveOurTool!"
        }
    }
    div {
        className = ClassName("row justify-content-center")
        h4 {
            +"Non-profit Opensource Ecosystem with a focus on finding code bugs".t()
        }
    }
    div {
        className = ClassName("row justify-content-center mt-2")
        cardUser { userInfo = props.userInfo }
        cardServiceInfo { }
        cardAboutUs { }
    }
    div {
        className = ClassName("row justify-content-center mt-5 text-gray-900")
        h2 {
            +"Notifications".t()
        }
    }
    div {
        className = ClassName("card mb-4 mr-3 ml-3")
        div {
            className = ClassName("card-body")
            p {
                +"Your notifications will be located here.".t()
            }
        }
    }
}

/**
 * @param img to show it with description on welcome view
 */
@Suppress("MAGIC_NUMBER")
internal fun ChildrenBuilder.cardImage(img: String) {
    img {
        src = img
        style = jso {
            height = 14.rem
            width = 14.rem
        }
    }
}
