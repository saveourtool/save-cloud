/**
 * Component for save-demo brief info rendering
 */

package com.saveourtool.save.frontend.components.basic.demo.welcome

import com.saveourtool.save.frontend.components.basic.markdown
import csstype.ClassName
import csstype.rem
import js.core.jso
import react.VFC
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.strong

private val saveDemoIntroMd = """
    |## save-demo
    |We are happy yo introduce you a new service - **save-demo**, which will help static analysis tool developers demonstrate their solutions. 
""".trimMargin()

private val saveDemoHowToMd = """
    |### How to create demo?
    |1. Create saveourtool [organization](#/create-organization) and [project](#/create-project);
    |2. Go to your project's demo tab;
    |3. Upload your tool, select sdk and set run commands;
    |4. Make sure all the inputted filenames match filenames in run commands; 
    |5. Create demo and run demo container.
    |
    |In case of any error feel free to [contact us](https://github.com/saveourtool/save-cloud/issues/new).
""".trimMargin()

@Suppress("MAGIC_NUMBER")
internal val introductionSection = VFC {
    div {
        className = ClassName("card flex-md-column mb-1 box-shadow")
        style = jso { minHeight = 25.rem }
        div {
            className = ClassName("card-body d-flex align-items-start")
            div {
                strong {
                    className = ClassName("d-inline-block mb-2 text-info")
                    +"Introducing"
                }
                markdown(saveDemoIntroMd)
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
            markdown(saveDemoHowToMd)
        }
    }
}
