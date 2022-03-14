package org.cqfn.save.gateway.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.security.web.util.matcher.IpAddressMatcher
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
        knownActuatorConsumers ?: return true

        return knownActuatorConsumers.split(',').map {
            IpAddressMatcher(it)
        }.any { it.matches(ipAddress?.address?.decodeToString()) }
    }

    /**
     * @property url URL of save-backend
     */
    data class Backend(
        val url: String,
    )
}
