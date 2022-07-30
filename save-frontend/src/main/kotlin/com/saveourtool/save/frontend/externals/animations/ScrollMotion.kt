/**
 * Very impressive and beautiful library with animation for scrolling:
 * https://github.com/1000ship/react-scroll-motion
 */

@file:JsModule("react-scroll-motion")
@file:JsNonModule

package com.saveourtool.save.frontend.externals.animations

import react.FC
import react.PropsWithChildren

// =================core==========================
@JsName("ScrollContainerProps")
external interface ScrollContainerProps : PropsWithChildren {
    var snap: String
    var scrollParent: dynamic
}

@JsName("ScrollPageProps")
external interface ScrollPageProps : PropsWithChildren {
    var debugBorder: Boolean
    var page: dynamic
}

@JsName("AnimatorProps")
external interface AnimatorProps : PropsWithChildren {
    var animation: Animation
}

@JsName("Animation")
external interface Animation

@JsName("ScrollContainer")
external val scrollContainer: FC<ScrollContainerProps>

@JsName("ScrollPage")
external val scrollPage: FC<ScrollPageProps>

@JsName("Animator")
external val animator: FC<AnimatorProps>

// =================sticky animation==========================
@JsName("Sticky")
external fun sticky(
    left: Number = definedExternally,
    top: Number = definedExternally,
): Animation

@JsName("StickyIn")
external fun stickyIn(
    left: Number = definedExternally,
    top: Number = definedExternally,
): Animation

@JsName("StickyOut")
external fun stickyOut(
    left: Number = definedExternally,
    top: Number = definedExternally,
): Animation

// =================fade animation==========================
@JsName("Fade")
external fun fade(
    from: Number = definedExternally,
    to: Number = definedExternally,
): Animation

@JsName("FadeIn")
external fun fadeIn(
    from: Number = definedExternally,
    to: Number = definedExternally
): Animation

@JsName("FadeOut")
external fun fadeOut(
    from: Number = definedExternally,
    to: Number = definedExternally
): Animation


// =================move animation==========================
@JsName("Move")
external fun move(
    dx: Number = definedExternally,
    dy: Number = definedExternally,
    outDx: Number = definedExternally,
    outDy: Number = definedExternally
): Animation


@JsName("MoveIn")
external fun moveIn(
    dx: Number = definedExternally,
    dy: Number = definedExternally
): Animation

@JsName("MoveOut")
external fun moveOut(
    dx: Number = definedExternally,
    dy: Number = definedExternally
): Animation

// =================zoom animation==========================
@JsName("Zoom")
external fun zoom(
    from: Number = definedExternally,
    to: Number = definedExternally,
): Animation

@JsName("ZoomIn")
external fun zoomIn(
    from: Number = definedExternally,
    to: Number = definedExternally,
): Animation

@JsName("ZoomOut")
external fun zoomOut(
    from: Number = definedExternally,
    to: Number = definedExternally,
): Animation

/**
 * @param animations
 * @return batched and merged animation from the list of several animations
 */
@JsName("batch")
external fun batch(vararg animations: Animation): Animation
