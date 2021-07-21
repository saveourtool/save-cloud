/**
 * SDK which are supported for test execution in save-cloud
 */

package org.cqfn.save.domain

import kotlinx.serialization.Serializable

/**
 * @property name name of the SDK
 */
@Serializable
open class Sdk(val name: String, open val version: String) {
    /**
     * Should be used when no particular SDK is required
     */
    object Default : Sdk("ubuntu", "latest")

    override fun toString(): String {
        return "$name:$version"
    }
}

/**
 * @property version version of JDK
 */
class Jdk(override val version: String) : Sdk("openjdk", version)

/**
 * @property version version of Python
 */
class Python(override val version: String) : Sdk("python", version)

fun String.toSdk() =
    when(this) {
        "Java 11" -> Jdk("11-jdk")
        "Java 8" -> Jdk("8-jdk")
        "Python 3.7" -> Python("3.7")
        else -> Sdk.Default
    }
