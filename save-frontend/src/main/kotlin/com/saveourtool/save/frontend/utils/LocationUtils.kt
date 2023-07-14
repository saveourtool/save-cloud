@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.utils

import remix.run.router.Location

/**
 * @param url url for comparison
 */
fun Location.not(url: String) = !pathname.startsWith(url)
