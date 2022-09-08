/**
 * This file contains util methods for kotlin
 */

package com.saveourtool.save.utils

/**
 * @return current value cast to [R] or null if casting is not possible
 */
inline fun <T : Any, reified R : Any> T.castOrNull(): R? = takeIf { it is R }?.let { it as R }
