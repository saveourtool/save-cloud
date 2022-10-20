@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.externals.calendar

import org.w3c.dom.events.Event
import react.ChildrenBuilder
import react.react
import kotlin.js.Date

/**
 * @param onChange
 * @param handler
 */
fun ChildrenBuilder.calendar(
    onChange: (Date, Event) -> Unit,
    handler: ChildrenBuilder.(ReactCalendarProps) -> Unit = {},
) {
    kotlinext.js.require("react-calendar/dist/Calendar.css")
    ReactCalendar::class.react {
        this.onChange = onChange
        this.showNeighboringMonth = false
        this.locale = "en-EN"
        handler(this)
    }
}
