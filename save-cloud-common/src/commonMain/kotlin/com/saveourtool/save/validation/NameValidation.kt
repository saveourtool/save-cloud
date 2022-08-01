/**
 * Module that implements for name checks.
 */

package com.saveourtool.save.validation

private fun String.hasOnlyLettersOrDigitsOrHyphens() = all { it.isLetterOrDigit() || it == '-' }

private fun String.containsForbiddenWords() = FrontendRoutes.getForbiddenWords().any { this == it }

/**
 * Check if name is valid.
 *
 * @param name string that should be checked
 * @return true if name is valid, false otherwise
 */
fun isNameValid(name: String) = name.run {
    isNotBlank() && first() != '-' && last() != '-' && hasOnlyLettersOrDigitsOrHyphens() && !containsForbiddenWords()
}
