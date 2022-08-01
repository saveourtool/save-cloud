/**
 * Module that implements different types of validation.
 */

package com.saveourtool.save.validation

/**
 * Default amount of characters allowed for names
 */
const val NAMING_ALLOWED_LENGTH = 64

/**
 * Check if name is valid.
 *
 * @param allowedLength maximum allowed number of characters, default [NAMING_ALLOWED_LENGTH]
 * @return true if name is valid, false otherwise
 */
fun String.isValidName(allowedLength: Int = NAMING_ALLOWED_LENGTH) = run {
    isNotBlank() && first() != '-' && last() != '-' && hasOnlyLettersOrDigitsOrHyphens() && !containsForbiddenWords() && isLengthOk(allowedLength)
}

/**
 * Check if url is valid.
 *
 * @return true if url is valid, false otherwise
 */
fun String.isValidUrl() = ValidationRegularExpressions.URL_VALIDATOR.value.matches(this)

/**
 * Check if email is valid.
 *
 * @return true if email is valid, false otherwise
 */
fun String.isValidEmail() = ValidationRegularExpressions.EMAIL_VALIDATOR.value.matches(this)

private fun String.hasOnlyLettersOrDigitsOrHyphens() = all { it.isLetterOrDigit() || it == '-' }

private fun String.containsForbiddenWords() = FrontendRoutes.getForbiddenWords().any { this == it }

private fun String.isLengthOk(allowedLength: Int) = length < allowedLength
