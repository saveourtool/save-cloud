@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.externals.lodash

const val DEFAULT_DEBOUNCE_PERIOD = 500

/**
 * @param function to be debounced
 * @param milliseconds between invocations
 * @return debounced function
 */
@JsModule("lodash.debounce")
@JsNonModule
@Suppress("LAMBDA_IS_NOT_LAST_PARAMETER")
external fun debounce(function: () -> Unit, milliseconds: Int): () -> Unit
