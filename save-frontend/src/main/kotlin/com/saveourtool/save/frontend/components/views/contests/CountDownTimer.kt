package com.saveourtool.save.frontend.components.views.contests

import com.saveourtool.save.entities.ContestDto
import react.FC
import react.Props
import kotlin.js.Date

val countDownFc = countDown()

external interface CountDownProps : Props {
    var contests: Array<ContestDto>
    // var countDownUpdate:  Unit = console.log()
}

fun countDown() = FC<CountDownProps> {props ->
    console.log(props.contests)
    props.contests.forEach {
        /*span {
          +it.name
        }*/
    }
    console.log("========")
    console.log(Date().toLocaleString())
}
