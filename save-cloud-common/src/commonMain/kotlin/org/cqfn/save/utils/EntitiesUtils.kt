@file:Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "FILE_NAME_MATCH_CLASS",
    "MatchingDeclarationName",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE"
)

package com.saveourtool.save.utils

expect enum class EnumType {
    /** Persist enumerated type property or field as an integer.  */
    ORDINAL,

    /** Persist enumerated type property or field as a string.  */
    STRING,
    ;
}

expect class LocalDateTime
