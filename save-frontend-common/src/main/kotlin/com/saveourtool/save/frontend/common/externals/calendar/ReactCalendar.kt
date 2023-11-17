@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")
@file:JsModule("react-calendar")
@file:JsNonModule

package com.saveourtool.save.frontend.common.externals.calendar

import org.w3c.dom.events.Event
import react.Component
import react.PropsWithChildren
import react.ReactElement
import react.State
import kotlin.js.Date

/**
 * External declaration of [ReactCalendarProps] react component
 */
@JsName("Calendar")
external class ReactCalendar : Component<ReactCalendarProps, State> {
    override fun render(): ReactElement<ReactCalendarProps>?
}

/**
 * Props of [ReactCalendarProps]
 */
external interface ReactCalendarProps : PropsWithChildren {
    /**
     * The beginning of a period that shall be displayed.
     * If you wish to use React-Calendar in an uncontrolled way, use [defaultActiveStartDate] instead.
     */
    var activeStartDate: Date

    /**
     * Class name(s) that will be added along with "react-calendar" to the main React-Calendar <div> element.
     */
    var className: String

    /**
     * The beginning of a period that shall be displayed by default.
     * If you wish to use React-Calendar in a controlled way, use [activeStartDate] instead.
     */
    var defaultActiveStartDate: Date

    /**
     * Calendar value that shall be selected initially.
     * Can be either one value or an array of two values.
     * If you wish to use React-Calendar in a controlled way, use [value] instead.
     */
    var defaultValue: Array<Date>

    /**
     * Function called when the user clicks an item (day on month view, month on year view and so on)
     * on the most detailed view available.
     */
    var onChange: (Date, Event) -> Unit

    /**
     * Function called when the user clicks a day.
     */
    var onClickDay: (Int, Event) -> Unit

    /**
     * Calendar value. Can be either one value or an array of two values.
     * If you wish to use React-Calendar in an uncontrolled way, use [defaultValue] instead.
     */
    var value: Array<Date>

    /**
     * Whether days from previous or next month shall be rendered
     * if the month doesn't start on the first day of the week
     * or doesn't end on the last day of the week, respectively.
     */
    var showNeighboringMonth: Boolean

    /**
     * Locale that should be used by the calendar.
     * Can be any IETF language tag.
     */
    var locale: String

    /**
     * Type of calendar that should be used. Can be "ISO 8601", "US", "Arabic", or "Hebrew".
     * Setting to "US" or "Hebrew" will change the first day of the week to Sunday.
     * Setting to "Arabic" will change the first day of the week to Saturday.
     * Setting to "Arabic" or "Hebrew" will make weekends appear on Friday to Saturday.
     */
    var calendarType: String
}
