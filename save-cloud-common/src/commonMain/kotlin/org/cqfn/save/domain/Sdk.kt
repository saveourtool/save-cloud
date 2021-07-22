/**
 * SDK which are supported for test execution in save-cloud
 */

package org.cqfn.save.domain

/**
 * @property name name of the SDK
 * @property version
 */
@Serializable
open class Sdk(val name: String, open val version: String) {
    /**
     * Should be used when no particular SDK is required
     */
    object Default : Sdk("ubuntu", "latest")

    override fun toString() = "$name:$version"

    companion object {
        val sdks = listOf("Default", "Java", "Python")
    }
}

/**
 * @property version version of JDK
 */
class Jdk(override val version: String) : Sdk("openjdk", version) {
    companion object {
        val versions = listOf("8", "11")
    }
}

/**
 * @property version version of Python
 */
class Python(override val version: String) : Sdk("python", version) {
    companion object {
        val versions = listOf("2.7", "3.7")
    }
}

/**
 * Parse string to sdk
 *
 * @return sdk by string
 */
fun String.toSdk(): Sdk {
    val splitSdk = this.split("$")
    require(splitSdk.size == 2) { "Cant find correct sdk and version" }
    val (sdkType, sdkVersion) = splitSdk.run { this.first() to this.last() }
    return when (sdkType) {
        "Java" -> Jdk(sdkVersion)
        "Python" -> Python(sdkVersion)
        else -> Sdk.Default
    }
}

/**
 * @return all version by sdk name
 */
fun String.getSdkVersion(): List<String> =
        when (this) {
            "Java" -> Jdk.versions
            "Python" -> Python.versions
            else -> listOf("latest")
        }
