@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.demo.utils

import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.demo.config.KubernetesConfig
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.KubernetesClient
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import okhttp3.internal.closeQuietly
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import reactor.core.publisher.Flux
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.random.Random

private const val DEFAULT_BUFFER_SIZE = 4096
private const val PORT_RANGE_FROM = 20000
private const val PORT_RANGE_TO = 30000

/**
 * Subdomain of all save-demo-agents
 */
const val CLUSTER_DOMAIN = "cluster.local"

private val logger = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

/**
 * Read bytes from [ByteReadChannel] as [Flux] of [ByteBuffer].
 * The input stream is closed when Flux is terminated.
 *
 * @return [Flux] of [ByteBuffer]s read from [ByteReadChannel]
 */
fun ByteReadChannel.toByteBufferFlux(): Flux<ByteBuffer> = DataBufferUtils.readInputStream(
    ::toInputStream,
    DefaultDataBufferFactory.sharedInstance,
    DEFAULT_BUFFER_SIZE,
).map { it.asByteBuffer() }

/**
 * Request wrapper that sets up port forwarding for pod with name [Pod.getFullResourceName], sends [request] to loopback
 * with path [urlPathSegments].
 *
 * Note that [request] returns object of generic type [T] allowing to either return HttpResponse or body of the response.
 *
 * @param urlPathSegments list of endpoint path segments (e.g. ["run"])
 * @param demoPod [Pod] with save-demo-agent that runs requested demo
 * @param request callback that receives [Url]
 * @return result of type [T]
 * @throws IOException rethrown from [request]
 */
private suspend fun <T> KubernetesClient.portForwardingRequest(
    urlPathSegments: List<String>,
    demoPod: Pod,
    request: suspend (Url) -> T,
) {
    val podName = demoPod.metadata.name
    val podResource = pods().withName(podName)
    val containerPort = demoPod.spec.containers.single()
        .ports
        .single()
        .containerPort
    val saveDemoPort = Random.nextInt(PORT_RANGE_FROM, PORT_RANGE_TO)
    logger.info("Forwarding port $saveDemoPort to $containerPort of container with pod $podName")
    return podResource.portForward(containerPort, saveDemoPort)
        .let { portForward ->
            try {
                request(
                    URLBuilder(
                        port = portForward.localPort,
                        pathSegments = urlPathSegments,
                    ).build()
                )
            } catch (ioException: IOException) {
                if (!portForward.isAlive) {
                    logger.error("Port-Forwarding tends to be broken.")
                }
                if (portForward.errorOccurred()) {
                    portForward.serverThrowables.forEach {
                        logger.error("SERVER THROWABLE: ${it.describe()}")
                    }
                    portForward.clientThrowables.forEach {
                        logger.error("CLIENT THROWABLE: ${it.describe()}")
                    }
                }
                throw ioException
            } finally {
                portForward.closeQuietly()
            }
        }
}

/**
 * Request wrapper that uses existing kubernetes service [KubernetesConfig.agentSubdomainName] created for all save-demo-agents,
 *   ip address of pod (Pod.status.podIp) and sends [request] to constructed url with path [urlPath].
 *
 * Url is constructed like this:
 *
 * {POD-IP}.{SUBDOMAIN-SERVICE}.{NAMESPACE}.svc.{CLUSTER.DOMAIN}
 *
 * Note that [request] returns object of generic type [R] allowing to either return HttpResponse or body of the response.
 *
 * @param urlPath endpoint path segments (e.g. "/run")
 * @param demoPod [Pod] with save-demo-agent that runs requested demo
 * @param kubernetesSettings kubernetes configuration
 * @param request callback that receives [Url]
 * @return result of type [R]
 */
suspend fun <R> demoAgentRequestWrapper(
    urlPath: String,
    demoPod: Pod,
    kubernetesSettings: KubernetesConfig,
    request: suspend (Url) -> R,
): R {
    val host = addressToDnsResolution(demoPod.status.podIP, kubernetesSettings)
    return request(
        URLBuilder(
            host = host,
            port = kubernetesSettings.agentPort,
        )
            .appendPathSegments(urlPath)
            .build()
    )
}

/**
 * Get DNS-resolvable name of pod that is controlled by headless Service
 *
 * Url is constructed like this:
 *
 * {POD-IP}.{SUBDOMAIN-SERVICE}.{NAMESPACE}.svc.{CLUSTER.DOMAIN}
 *
 * @param podIp ip of a pod inside kubernetes cluster
 * @param kubernetesSettings kubernetes configuration
 * @return DNS-resolvable name of pod in format {POD-IP}.{SUBDOMAIN-SERVICE}.{NAMESPACE}.svc.{CLUSTER.DOMAIN}
 */
fun addressToDnsResolution(podIp: String, kubernetesSettings: KubernetesConfig) = listOf(
    podIp.replace(".", "-"),
    kubernetesSettings.agentSubdomainName,
    kubernetesSettings.namespace,
    "svc",
    CLUSTER_DOMAIN,
)
    .joinToString(".")
