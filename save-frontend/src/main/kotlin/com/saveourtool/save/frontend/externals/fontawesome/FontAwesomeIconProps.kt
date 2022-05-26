package com.saveourtool.save.frontend.externals.fontawesome

import react.PropsWithChildren

/**
 * RProps of [FontAwesomeIcon]
 */
external interface FontAwesomeIconProps : PropsWithChildren {
    /**
     * Icon. Can be an object, string or array.
     */
    var icon: dynamic

    /**
     * Classes of the element
     */
    var className: String

    /**
     * Color of the element
     */
    var color: String
}
