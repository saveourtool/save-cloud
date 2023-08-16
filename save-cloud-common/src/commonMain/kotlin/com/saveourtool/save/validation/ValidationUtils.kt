/**
 * Module that implements different types of validation.
 */

package com.saveourtool.save.validation

/**
 * Default amount of characters allowed for names
 */
const val NAMING_ALLOWED_LENGTH = 64
const val NAMING_MAX_LENGTH = 22
private val namingAllowedSpecialSymbols = setOf('-', '_', '.')

/**
 * Check if name is valid.
 *
 * @param allowedLength maximum allowed number of characters, default [NAMING_ALLOWED_LENGTH]
 * @return true if name is valid, false otherwise
 */
fun String.isValidName(allowedLength: Int = NAMING_ALLOWED_LENGTH) = run {
    isNotBlank() && setOf(first(), last()).none { it in namingAllowedSpecialSymbols } &&
            hasOnlyAlphaNumOrAllowedSpecialSymbols() && !containsForbiddenWords() && isLengthOk(allowedLength)
}

/**
 * Check if path is valid.
 *
 * @param isRelative if true, check is done for relative paths, otherwise checking absolute paths
 * @return true if path is valid, false otherwise
 */
fun String.isValidPath(isRelative: Boolean = true) = run {
    isNotBlank() && if (isRelative) {
        first() != '/' && ValidationRegularExpressions.RELATIVE_PATH_VALIDATOR.value.matches(this)
    } else {
        first() == '/' && ValidationRegularExpressions.ABSOLUTE_PATH_VALIDATOR.value.matches(this)
    }
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

/**
 * Check if length of name is valid.
 *
 * @return true if length name less than [NAMING_MAX_LENGTH], false otherwise
 */
fun String.isValidLengthName() = isLengthOk(NAMING_MAX_LENGTH)

/**
 * checking that the field is less than NAMING_ALLOWED_LENGTH symbols
 *
 * @return false if the length is more than [NAMING_ALLOWED_LENGTH]
 */
fun String.isValidMaxAllowedLength() = isLengthOk(NAMING_ALLOWED_LENGTH)

private fun String.hasOnlyAlphaNumOrAllowedSpecialSymbols() = all { it.isLetterOrDigit() || namingAllowedSpecialSymbols.contains(it) }

private fun String.containsForbiddenWords() = (FrontendRoutes.getForbiddenWords() + BackendRoutes.getForbiddenWords())
    .any { this == it }

private fun String.isLengthOk(allowedLength: Int) = length < allowedLength
