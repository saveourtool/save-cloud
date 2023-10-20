/**
 * Utility file with validation functions that should be passed to inputForm
 */

package com.saveourtool.save.frontend.components.views.usersettings.right.validation

import com.saveourtool.save.frontend.utils.UsefulUrls
import com.saveourtool.save.validation.*

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
        if (!isValidName(NAMING_ALLOWED_LENGTH, setOf('-', '_', '.', ' '))) "Name should contain only English letters and be less than $NAMING_ALLOWED_LENGTH symbols" else ""

/**
 * @return validation in inputField
 */
fun String.validateCompany(): String =
        if (!isValidName()) "Affiliation should contain only English letters and be less than $NAMING_ALLOWED_LENGTH symbols" else ""

/**
 * @return validation in inputField
 */
fun String.validateLocation(): String =
        if (!isValidName(NAMING_ALLOWED_LENGTH, setOf('-', '_', '.', ' ', ','))) {
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
            this.matches(UsefulUrls.LINKEDIN.regex) -> ""
            else -> "Url should start with ${UsefulUrls.LINKEDIN.basicUrl}"
        }

/**
 * @return validation in inputField
 */
fun String.validateGithub(): String =
        when {
            this == "" -> ""
            this.matches(UsefulUrls.GITHUB.regex) || this.matches(UsefulUrls.GITEE.regex) -> ""
            else -> "Url should start with ${UsefulUrls.GITEE.basicUrl} or ${UsefulUrls.GITHUB.basicUrl}"
        }

/**
 * @return validation in inputField
 */
fun String.validateTwitter(): String =
        when {
            this == "" -> ""
            this.matches(UsefulUrls.XCOM.regex) || this.matches(UsefulUrls.TWITTER.regex) -> ""
            else -> "Url should start with ${UsefulUrls.XCOM.basicUrl} or ${UsefulUrls.TWITTER.basicUrl}"
        }
