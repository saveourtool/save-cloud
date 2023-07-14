package com.saveourtool.save.frontend.utils

import remix.run.router.Location

/**
 * @param url url for comparison
 */
fun Location.not(url: String) = !pathname.startsWith(url)
