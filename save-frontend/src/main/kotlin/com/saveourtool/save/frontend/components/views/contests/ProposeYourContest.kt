/**
 * Card with our e-mail - where you can propose a contest
 */

package com.saveourtool.save.frontend.components.views.contests

import csstype.ClassName
import csstype.rem
import react.ChildrenBuilder
import react.dom.html.ReactHTML.div

import kotlinx.js.jso

/**
 * rendering of a card where we suggest to propose new custom contests
 */
@Suppress("MAGIC_NUMBER")
fun ChildrenBuilder.proposeContest() {
    div {
        className = ClassName("col-lg-2")
        div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 7.rem
            }

            // FixMe: Want to propose contest? Write e-mail! undraw_mailbox_re_dvds.svg
        }
    }
}
