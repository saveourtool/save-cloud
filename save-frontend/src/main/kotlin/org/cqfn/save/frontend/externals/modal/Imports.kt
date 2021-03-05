//@file:JsModule("react-modal")

package org.cqfn.save.frontend.externals.modal

import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import react.Component
import react.RProps
import react.RState
import react.ReactElement

external interface ModalProps : RProps {
    /* Boolean describing if the modal should be shown or not. Defaults to false. */
    var isOpen: Boolean

    /* String indicating how the content container should be announced to screenreaders. */
    var contentLabel: String
}

external interface Styles {
}

external interface Classes {
    var base: String
    var afterOpen: String
    var beforeClose: String
}

/** Describes overlay and content element references passed to onAfterOpen function */
external interface OnAfterOpenCallbackOptions {
    /** overlay element reference */
    var overlayEl: Element
    /** content element reference */
    var contentEl: HTMLDivElement;
}

/** Describes unction that will be run after the modal has opened */
external interface OnAfterOpenCallback {
    fun onAfterOpen(obj: OnAfterOpenCallbackOptions): Unit
}

@JsModule("react-modal")
@JsNonModule
@JsName("ModalPortal")
external class ReactModalPortal : Component<RProps, RState> {
    override fun render(): ReactElement?

    val overlay: HTMLDivElement
    val content: HTMLDivElement
}

@JsModule("react-modal")
@JsNonModule
@JsName("ReactModal")
external class ReactModal : Component<ModalProps, RState> {
    override fun render(): ReactElement?

    var portal: ReactModalPortal? = definedExternally

    companion object {
        /* Override base styles for all instances of this component. */
        val defaultStyles: Styles

        /**
         * Call this to properly hide your application from assistive screenreaders
         * and other assistive technologies while the modal is open.
         */
        fun setAppElement(appElement: HTMLElement): Unit
    }
}
