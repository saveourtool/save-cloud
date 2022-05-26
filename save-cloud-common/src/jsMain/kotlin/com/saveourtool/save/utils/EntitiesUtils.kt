@file:Suppress("MISSING_KDOC_TOP_LEVEL", "FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.save.utils

actual typealias LocalDateTime = kotlinx.datetime.LocalDateTime

actual enum class EnumType {
    /** Persist enumerated type property or field as an integer.  */
    ORDINAL,

    /** Persist enumerated type property or field as a string.  */
    STRING,
    ;
}
