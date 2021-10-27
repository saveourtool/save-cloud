@file:Suppress("MISSING_KDOC_TOP_LEVEL", "FILE_NAME_MATCH_CLASS")

package org.cqfn.save.utils

expect enum class EnumType {
    /** Persist enumerated type property or field as an integer.  */
    ORDINAL,

    /** Persist enumerated type property or field as a string.  */
    STRING,
    ;
}
