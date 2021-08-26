package org.cqfn.save.frontend.externals.fontawesome

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
}
