@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.cvsscalculator

import kotlinx.serialization.Serializable

/**
 * Type of Attack Vector
 *
 * @property value abbreviated value from the cvss vector
 */
@Serializable
enum class AttackVectorType(val value: String) {
    ADJACENT_NETWORK("A"),
    LOCAL("L"),
    NETWORK("N"),
    NOT_DEFINED("_"),
    PHYSICAL("P"),
    ;
}

/**
 * Type of Attack Complexity
 *
 * @property value abbreviated value from the cvss vector
 */
@Serializable
enum class AttackComplexityType(val value: String) {
    HIGH("H"),
    LOW("L"),
    NOT_DEFINED("_"),
    ;
}

/**
 * Type of Privileges Required
 *
 * @property value abbreviated value from the cvss vector
 */
@Serializable
enum class PrivilegesRequiredType(val value: String) {
    HIGH("H"),
    LOW("L"),
    NONE("N"),
    NOT_DEFINED("_"),
    ;
}

/**
 * Type of User Interaction
 *
 * @property value abbreviated value from the cvss vector
 */
@Serializable
enum class UserInteractionType(val value: String) {
    NONE("N"),
    NOT_DEFINED("_"),
    REQUIRED("R"),
    ;
}

/**
 * Type of Scope
 *
 * @property value abbreviated value from the cvss vector
 */
@Serializable
enum class ScopeType(val value: String) {
    CHANGED("C"),
    NOT_DEFINED("_"),
    UNCHANGED("U"),
    ;
}

/**
 * Type of CIA
 *
 * @property value abbreviated value from the cvss vector
 */
@Serializable
enum class CiaType(val value: String) {
    HIGH("H"),
    LOW("L"),
    NONE("N"),
    NOT_DEFINED("_"),
    ;
}

/**
 * Version of CVSS
 *
 * @property value abbreviated value from the cvss version
 */
@Serializable
enum class CvssVersion(val value: String) {
    THREE("3.0"),
    THREE_ONE("3.1"),
    TWO("2.0"),
    ;
}
