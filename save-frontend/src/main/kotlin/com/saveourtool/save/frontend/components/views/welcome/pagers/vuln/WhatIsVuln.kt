/**
 * [1 page] Main information about SAVE-cloud
 */

package com.saveourtool.save.frontend.components.views.welcome.pagers.vuln

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

private const val MANY_INDEPENDENT_REPOS = """
    Independent vulnerability repositories underscore the need for a unified source.
    Consolidating vulnerabilities into one platform promotes collaboration, streamlines critical information access,
    and simplifies identification and mitigation of security flaws.
    It enhances vulnerability management and keeps organizations updated on the latest threats from a reliable,
    centralized source.
"""

private const val WHY_VULNERABILITIES_DANGEROUS = """
    Vulnerabilities allow attackers to exploit weaknesses in IT systems, leading to unauthorized access,
    data manipulation, operational disruptions, and potential financial, reputational, and legal consequences.
    Timely vulnerability detection and mitigation are crucial for minimizing cyberattack risks and preserving
    IT system security and integrity.
"""

private const val IMPORTANCE_OF_VULNERABILITY_ARCHIVES = """
    A vulnerability archive is vital as a centralized repository for documented vulnerabilities.
    It offers insights for security professionals, aids in proactive risk management,
    and enables timely vulnerability identification and mitigation.
    It also enhances understanding of trends, patterns, and common vulnerabilities,
    fortifying overall security posture against future threats.
"""

/**
 * rendering of 4 paragraphs with info about SAVE
 */
fun ChildrenBuilder.renderVulnerabilityGeneralInfo() {
    div {
        style = jso {
            color = "rgb(6, 7, 89)".unsafeCast<Color>()
        }
        className = ClassName("row justify-content-center")

        div {
            className = ClassName("row justify-content-center mt-5 mx-4")
            textCard("What is vulnerability?", "img/undraw_question.svg", WHAT_IS_VULNERABILITY)
            textCard("Why should we pay attention?", "img/undraw_warning.svg", WHY_VULNERABILITIES_DANGEROUS)
        }

        div {
            className = ClassName("row justify-content-center mt-5 mx-4")
            textCard("Why vulnerability archives important?", "img/undraw_important.svg", IMPORTANCE_OF_VULNERABILITY_ARCHIVES)
            textCard("Unifying repositories", "img/undraw_share.svg", MANY_INDEPENDENT_REPOS)
        }
    }
}

private fun ChildrenBuilder.textCard(
    title: String,
    imageUrl: String,
    textStr: String,
) {
    div {
        className = ClassName("card border border-primary rounded rounded-pill col-5 mx-3")
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
