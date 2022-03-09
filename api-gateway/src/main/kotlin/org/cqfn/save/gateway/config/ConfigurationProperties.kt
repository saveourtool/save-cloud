package org.cqfn.save.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.net.InetAddress

/**
 * @property backend properties for connection to save-backend
 * @property basicCredentials space-separated username and password for technical user to access actuator
 * @property knownActuatorConsumers comma-separated list of IPs of clients that are allowed to access spring boot actuator
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "gateway")
data class ConfigurationProperties(
    val backend: Backend,
    val basicCredentials: String?,
    val knownActuatorConsumers: String?,
) {
    /**
     * @param ipAddress IP address to check against [knownActuatorConsumers]
     * @return true if [knownActuatorConsumers] are not specified (effectively, this check is turned off) or if
     * [ipAddress] matches one from that list. False otherwise
     */
    fun isKnownActuatorConsumer(ipAddress: InetAddress?): Boolean {
        knownActuatorConsumers ?: run {
            return true
        }
        return ipAddress in knownActuatorConsumers.split(',').map {
            InetAddress.getByName(it)
        }
    }

    /**
     * @property url URL of save-backend
     */
    data class Backend(
        val url: String,
    )
}
