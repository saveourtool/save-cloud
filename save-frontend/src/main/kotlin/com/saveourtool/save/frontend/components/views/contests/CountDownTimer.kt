/**
 * Countdown timer FC that is used to show how much time is left for this particular contest.
 * The idea is to motivate user to participate in contests from this view.
 */

@file:Suppress("diktat")

package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.contest.ContestDto
import react.FC
import react.Props
import kotlin.js.Date

/**
 * constant component
 */
val countDownFc = countDownTimer()

external interface CountDownProps : Props {
    var contests: Array<ContestDto>
}

/**
 * @return
 */
fun countDownTimer() = FC<CountDownProps> {props ->
    console.log(Date().toLocaleString())
    TODO("This will be finished in phaze 2: this countdown timer should be in a featured contest")
}
