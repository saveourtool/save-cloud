package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.ContestDto
import react.FC
import react.Props
import kotlin.js.Date

val countDownFc = countDown()

external interface CountDownProps : Props {
    var contests: Array<ContestDto>
}

fun countDown() = FC<CountDownProps> {props ->
    console.log(Date().toLocaleString())
    TODO("This will be finished in phaze 2: this countdown timer should be in a featured contest")
}
