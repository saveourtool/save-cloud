package org.cqfn.save.frontend.externals.fontawesome

import react.RProps

/**
 * RProps of [FontAwesomeIcon]
 */
external interface FontAwesomeIconProps : RProps {
    /**
     * Icon. Can be object, string or array.
     */
    var icon: dynamic

    /**
     * Classes of the element
     */
    var className: String
}
