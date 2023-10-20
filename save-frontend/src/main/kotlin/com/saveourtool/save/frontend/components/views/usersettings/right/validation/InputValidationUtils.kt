/**
 * Utility file with validation functions that should be passed to inputForm
 */

package com.saveourtool.save.frontend.components.views.usersettings.right.validation

import com.saveourtool.save.frontend.utils.UsefulUrls
import com.saveourtool.save.validation.*

private val namingAllowedSymbols = setOf('-', '_', '.', ' ')

/**
 * @return validation in inputField
 */
fun String.validateLogin(): String = if (isValidName(NAMING_MAX_LENGTH)) "" else NAME_ERROR_MESSAGE

/**
 * @return validation in inputField
 */
fun String.validateUserEmail(): String = if (isValidEmail()) "" else EMAIL_ERROR_MESSAGE

/**
 * @return validation in inputField
 */
fun String.validateRealName(): String =
        if (!isValidName(NAMING_ALLOWED_LENGTH, namingAllowedSymbols)) "Name should contain only English letters and be less than $NAMING_ALLOWED_LENGTH symbols" else ""

/**
 * @return validation in inputField
 */
fun String.validateCompany(): String =
        if (!isValidName(NAMING_ALLOWED_LENGTH, namingAllowedSymbols)) "Affiliation should contain only English letters and be less than $NAMING_ALLOWED_LENGTH symbols" else ""

/**
 * @return validation in inputField
 */
fun String.validateLocation(): String =
        if (!isValidName(NAMING_ALLOWED_LENGTH, namingAllowedSymbols + ',')) {
            "Location should contain only English letters and be less than $NAMING_ALLOWED_LENGTH symbols"
        } else {
            ""
        }

/**
 * @return validation in inputField
 */
fun String.validateWebsite(): String = if (isValidUrl()) "" else URL_ERROR_MESSAGE

/**
 * @return validation in inputField
 */
fun String.validateLinkedIn(): String =
        when {
            this == "" -> ""
            this.startsWith(UsefulUrls.LINKEDIN.value) -> ""
            else -> "Url should start with ${UsefulUrls.LINKEDIN.value}"
        }

/**
 * @return validation in inputField
 */
fun String.validateGithub(): String =
        when {
            this == "" -> ""
            this.startsWith(UsefulUrls.GITHUB.value) || this.startsWith(UsefulUrls.GITEE.value) -> ""
            else -> "Url should start with ${UsefulUrls.GITEE.value} or ${UsefulUrls.GITHUB.value}"
        }

/**
 * @return validation in inputField
 */
fun String.validateTwitter(): String =
        when {
            this == "" -> ""
            this.startsWith(UsefulUrls.XCOM.value) || this.startsWith(UsefulUrls.TWITTER.value) -> ""
            else -> "Url should start with ${UsefulUrls.XCOM.value} or ${UsefulUrls.TWITTER.value}"
        }
