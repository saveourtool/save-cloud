/**
 * Module that implements for name checks.
 */

package com.saveourtool.save.validation

import com.saveourtool.save.utils.URL_PATH_DELIMITER

private fun String.hasOnlyLettersOrDigitsOrHyphens() = all { it.isLetterOrDigit() || it == '-' }

private fun String.containsForbiddenWords(additionalForbiddenWords: List<String> = emptyList()) = FrontendRoutes.values()
    .map {
        it.path.split(URL_PATH_DELIMITER)
    }
    .map {
        it + additionalForbiddenWords
    }
    .flatten()
    .any {
        this == it
    }

/**
 * Check if name is valid.
 *
 * @param name string that should be checked
 * @return true if name is valid, false otherwise
 */
fun isNameValid(name: String) = name.run {
    isNotBlank() && first() != '-' && last() != '-' && hasOnlyLettersOrDigitsOrHyphens() && !containsForbiddenWords()
}
