@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic

import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.strong
import web.cssom.ClassName

private const val SAVE_FOSS_GRAPH_INTRO_MD = """
    |## FOSS graph
    |Current page provides the list of publicly disclosed information security vulnerabilities and exposures.
"""

private const val SAVE_FOSS_GRAPH_ADD_NEW_MD = """
    |### New vulnerability
    |You can suggest your own [new vulnerability](#/vuln/create-vulnerability), if you didn't find the required one in our list.
    |After our approval, it will appear in the database.
"""

private const val SAVE_FOSS_GRAPH_HOW_TO_MD = """
    |### How to add vulnerability in project?
    |1. Create saveourtool [organization](#/create-organization) and [project](#/create-project);
    |2. Go to your project's security tab;
    |3. Create new problem and add vulnerability number;
    |
    |In case of any error feel free to [contact us](https://github.com/saveourtool/save-cloud/issues/new).
"""

private const val SAVE_FOSS_GRAPH_TOP_RATING_MD = """
    |### Top rating
    |You can see the [top rating](#/top-rating) of users and organizations.
"""

@Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
)
fun ChildrenBuilder.fossGraphIntroductionComponent() {
    div {
        className = ClassName("card flex-md-column box-shadow")
        div {
            className = ClassName("card-body d-flex align-items-start")
            div {
                strong {
                    className = ClassName("d-inline-block mb-2 text-info")
                    +"Introducing"
                }
                markdown(SAVE_FOSS_GRAPH_INTRO_MD.trimMargin())
            }
            div {
                className = ClassName("card-img-right flex-column d-none d-md-block")
                img {
                    className = ClassName("img-fluid")
                    src = "img/undraw_happy_announcement_re_tsm0.svg"
                }
            }
        }
        div {
            className = ClassName("card-body pt-0 pb-1")
            markdown(SAVE_FOSS_GRAPH_ADD_NEW_MD.trimMargin())
        }
        div {
            className = ClassName("card-body pt-0 pb-1")
            markdown(SAVE_FOSS_GRAPH_HOW_TO_MD.trimMargin())
        }
        div {
            className = ClassName("card-body pt-0 pb-1")
            markdown(SAVE_FOSS_GRAPH_TOP_RATING_MD.trimMargin())
        }
    }
}
