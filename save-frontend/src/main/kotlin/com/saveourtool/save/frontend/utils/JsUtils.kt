/**
 * Utilities for kotlin-js interop
 */

package com.saveourtool.save.frontend.utils

import react.ChildrenBuilder

import kotlinx.js.Object

/**
 * Shortcut for
 * ```kotlin
 * child(MyComponent::class) {
 *     spread(props) { key, value ->
 *         attrs[key] = value
 *     }
 * }
 * ```
 *
 * Allows writing `<MyComponent ...props/>` as
 * ```kotlin
 * child(MyComponent::class) {
 *     spread(props)
 * }
 * ```
 *
 * @param jsObject a JS object properties of which will be used
 */
fun ChildrenBuilder.spread(jsObject: Any) {
    spread(jsObject) { key, value ->
        asDynamic()[key] = value
    }
}

/**
 * Attempt to mimic `...` operator from ES6.
 * For example, equivalent of `<MyComponent ...props/>` would be
 * ```kotlin
 * child(MyComponent::class) {
 *     spread(props) { key, value ->
 *         attrs[key] = value
 *     }
 * }
 * ```
 *
 * @param jsObject a JS object which properties will be used
 * @param handler a handler for [jsObject]'s property names and values
 */
@Suppress("TYPE_ALIAS")
fun spread(jsObject: Any, handler: (key: String, value: Any) -> Unit) {
    Object.keys(jsObject).map {
        it to jsObject.asDynamic()[it] as Any
    }
        .forEach { (key, value) ->
            handler(key, value)
        }
}
