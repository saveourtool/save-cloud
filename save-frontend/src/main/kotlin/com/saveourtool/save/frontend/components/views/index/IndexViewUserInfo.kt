/**
 * Authorization component (Oauth2 elements) for Index View
 */

package com.saveourtool.save.frontend.components.views.index

import js.core.jso
import react.ChildrenBuilder
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img
import web.cssom.*

const val INDEX_VIEW_CUSTOM_BG = "rgb(247, 250, 253)"

val indexViewInfo: FC<IndexViewProps> = FC { props ->
    div {
        className = ClassName("row justify-content-center mt-5 text-gray-900")
        h2 {
            +"SaveOurTool!"
        }
    }
    div {
        className = ClassName("row justify-content-center")
        h4 {
            +"Non-profit Opensource Ecosystem with a focus on finding code bugs"
        }
    }
    div {
        className = ClassName("row justify-content-center mt-2")
        cardUser { userInfo = props.userInfo }
        cardServiceInfo { userInfo = props.userInfo }
        cardAboutUs { userInfo = props.userInfo }
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
