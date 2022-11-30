/**
 * Module that implements Validation with regular expression
 */

package com.saveourtool.save.validation

/**
 * @property value [Regex] that is used during validation
 */
enum class ValidationRegularExpressions(val value: Regex) {
    ABSOLUTE_PATH_VALIDATOR("(/[^/ \\n]+/?)*[^/ \\n]*".toRegex()),

    EMAIL_VALIDATOR("^\\w+([-+.']\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*\$".toRegex()),

    RELATIVE_PATH_VALIDATOR("([^/ \\n]+/?)*[^/ \\n]+".toRegex()),

    URL_VALIDATOR("((http|https)://)(www.)?[a-zA-Z\\d@:%._\\\\+~#?&/=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z\\d@:%._\\\\+~#?&/=]*)".toRegex()),
    ;
}
