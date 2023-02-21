@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.demo.utils

import com.saveourtool.save.core.logging.describe
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

private val logger = LoggerFactory.getLogger("c.s.s.d.u.HttpUtils")

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
 * Note that [request] returns object of generic type [R] allowing to either return HttpResponse or body of the response.
 *
 * @param urlPathSegments list of endpoint path segments (e.g. ["run"])
 * @param demoPod [Pod] with save-demo-agent that runs requested demo
 * @param kc [KubernetesClient] that is required for kubernetes interactions
 * @param request callback that receives url and port
 * @return result of type [R]
 * @throws IOException rethrown from [request]
 */
suspend fun <R> demoAgentRequestWrapper(
    urlPathSegments: List<String>,
    demoPod: Pod,
    kc: KubernetesClient,
    request: suspend (Url) -> R,
): R {
    val podName = demoPod.metadata.name
    val podResource = kc.pods().withName(podName)
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
