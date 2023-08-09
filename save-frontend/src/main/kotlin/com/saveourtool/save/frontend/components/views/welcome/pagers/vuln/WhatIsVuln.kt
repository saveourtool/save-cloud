/**
 * [1 page] Main information about SAVE-cloud
 */

package com.saveourtool.save.frontend.components.views.welcome.pagers.vuln

import com.saveourtool.save.frontend.components.basic.markdown
import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p
import web.cssom.*

private const val WHAT_IS_VULNERABILITY = """
     Vulnerability is a weakness or flaw in a system, network, software, or hardware that can
     be exploited by unauthorized individuals or malicious software to gain unauthorized access,
     disrupt operations, or steal sensitive information.
     Vulnerabilities can arise from programming errors, misconfigurations, outdated software, or design flaws.
"""

private const val IMPORTANCE_OF_VULNERABILITY_ARCHIVES = """
    A vulnerability archive is vital as a centralized repository for documented vulnerabilities.
    It offers insights for security professionals, aids in proactive risk management,
    and enables timely vulnerability identification and mitigation.
    It also enhances understanding of trends, patterns, and common vulnerabilities,
    fortifying overall security posture against future threats.
"""

private const val USEFUL_LINKS_TEXT = """
    * [OSV Schema](https://ossf.github.io/osv-schema/) - offers a data format interpretable by humans and machines.
    
    * [COSV Schema 1.0](https://mp.weixin.qq.com/s/1aJT1X09SVQeNzL8eHWT0Q) - enhances open-source vulnerability descriptions,
    promotes standardized data sharing for supply chain security, and operational efficiency.
    
    * [osv4k](https://github.com/saveourtool/osv4k) - Kotlin and Java model for the serialization and deserialization of OSV Schema.
"""

/**
 * rendering 4 paragraphs with info about VULN
 */
fun ChildrenBuilder.renderVulnerabilityGeneralInfo() {
    div {
        style = jso { color = "rgb(6, 7, 89)".unsafeCast<Color>() }

        div {
            className = ClassName("row justify-content-between mt-5")
            textCard("What is vulnerability?", "/img/undraw_question.svg", WHAT_IS_VULNERABILITY, "mr-3")
            textCard("Why vulnerability archives important?", "/img/undraw_share.svg", IMPORTANCE_OF_VULNERABILITY_ARCHIVES, "ml-3")
        }

        div {
            className = ClassName("row mt-4 align-middle")
            div {
                mdCard("Useful links", "/img/undraw_important.svg", USEFUL_LINKS_TEXT.trimIndent())
            }
        }
    }
}

private fun ChildrenBuilder.textCard(
    title: String,
    imageUrl: String,
    textStr: String,
    classes: String,
) {
    div {
        className = ClassName("card border border-primary rounded rounded-pill col $classes")
        div {
            className = ClassName("d-flex justify-content-center")
            img {
                className = ClassName("rounded m-3")
                src = imageUrl
                style = jso {
                    @Suppress("MAGIC_NUMBER")
                    height = 9.rem
                }
            }
        }
        h5 {
            style = jso {
                textAlign = TextAlign.center
            }
            +title
        }
        p {
            +textStr
        }
    }
}

private fun ChildrenBuilder.mdCard(
    title: String,
    imageUrl: String,
    markdownStr: String,
) {
    div {
        className = ClassName("card border border-primary rounded rounded-pill col")
        div {
            className = ClassName("row")
            div {
                className = ClassName("col-3 d-flex align-items-center")
                div {
                    className = ClassName("")
                    img {
                        className = ClassName("rounded mx-5 my-3")
                        src = imageUrl
                        style = jso {
                            @Suppress("MAGIC_NUMBER")
                            height = 8.rem
                        }
                    }
                    h5 {
                        style = jso { textAlign = TextAlign.center }
                        +title
                    }
                }
            }
            div {
                className = ClassName("col-8 align-middle m-3")
                markdown(markdownStr)
            }
        }
    }
}
