/**
 * Very impressive and beautiful library with animation for scrolling:
 * https://github.com/1000ship/react-scroll-motion
 */

@file:JsModule("react-scroll-motion")
@file:JsNonModule

package com.saveourtool.save.frontend.externals.animations

import react.FC
import react.PropsWithChildren

@JsName("ScrollContainer")
external val scrollContainer: FC<ScrollContainerProps>

@JsName("ScrollPage")
external val scrollPage: FC<ScrollPageProps>

@JsName("Animator")
external val animator: FC<AnimatorProps>

@JsName("Fade")
external val fade: Animation

@JsName("Move")
external val move: Animation

@JsName("Sticky")
external val sticky: Animation

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

/**
 * @param animations
 * @return batched and merged animation from the list of several animations
 */
@JsName("batch")
external fun batch(vararg animations: Animation): Animation
