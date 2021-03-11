/**
 * Kotlin/JS-React wrappers for react-modal library: JS definitions
 */

@file:Suppress("USE_DATA_CLASS")

package org.cqfn.save.frontend.externals.modal

import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import react.Component
import react.RProps
import react.RState
import react.ReactElement

/**
 * [RProps] of modal component
 */
external interface ModalProps : RProps {
    /**
     * Boolean describing if the modal should be shown or not. Defaults to false.
     */
    var isOpen: Boolean

    /**
     * String indicating how the content container should be announced to screenreaders.
     */
    var contentLabel: String

    /** String or object className to be applied to the modal content. */
    val className: Classes

    /** String or object className to be applied to the overlay. */
    val overlayClassName: Classes
}

/**
 * Styles of Modal component. Are represented as css types, described in csstype library.
 * // FixMe: add to kotlin somehow.
 */
external interface Styles

/**
 * The value corresponding to each key is a class name.
 */
external interface Classes {
    /**
     * This class will always be applied to the component
     */
    var base: String

    /**
     * This class will be applied after the modal has been opened
     */
    var afterOpen: String

    /**
     * This class will be applied after the modal has requested to be closed
     * (e.g. when the user presses the escape key or clicks on the overlay).
     * Will have no effect unless the closeTimeoutMS prop is set to a non-zero value, since otherwise the modal will be closed immediately when requested.
     */
    var beforeClose: String
}

/** Describes overlay and content element references passed to onAfterOpen function */
external interface OnAfterOpenCallbackOptions {
    /**
     * overlay element reference
     */
    var overlayEl: Element

    /**
     * content element reference
     */
    var contentEl: HTMLDivElement
}

/** Describes unction that will be run after the modal has opened */
external interface OnAfterOpenCallback {
    /**
     * @param obj
     */
    fun onAfterOpen(obj: OnAfterOpenCallbackOptions): Unit
}

@JsModule("react-modal")
@JsNonModule
@JsName("ModalPortal")
@Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_CLASS_ELEMENTS")  // todo figure out what is portal and describe
external class ReactModalPortal : Component<RProps, RState> {
    val content: HTMLDivElement
    val overlay: HTMLDivElement

    override fun render(): ReactElement?
}

/**
 * A main [Component] of react-modal.
 */
@JsModule("react-modal")
@JsNonModule
@JsName("ReactModal")
external class ReactModal : Component<ModalProps, RState> {
    /**
     * A portal for modal window.
     */
    var portal: ReactModalPortal? = definedExternally

    override fun render(): ReactElement?

    companion object {
        /**
         * Override base styles for all instances of this component.
         */
        val defaultStyles: Styles

        /**
         * Call this to properly hide your application from assistive screenreaders
         * and other assistive technologies while the modal is open.
         *
         * @param appElement an [HTMLElement] corresponding to app root
         */
        fun setAppElement(appElement: HTMLElement): Unit
    }
}
