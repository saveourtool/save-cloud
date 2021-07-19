/**
 * SDK which are supported for test execution in save-cloud
 */

package org.cqfn.save.domain

/**
 * @property name name of the SDK
 */
sealed class Sdk(val name: String) {
    /**
     * Should be used when no particular SDK is required
     */
    object Default : Sdk("default")
}

/**
 * @property version version of JDK
 */
data class Jdk(val version: String) : Sdk("java")

/**
 * @property version version of Python
 */
data class Python(val version: String) : Sdk("python")
