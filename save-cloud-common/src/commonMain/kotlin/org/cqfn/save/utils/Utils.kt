package org.cqfn.save.utils

/**
 * @param default
 * @return
 */
fun String?.ifNullOrEmpty(default: () -> String) = (this ?: "").ifBlank(default)
