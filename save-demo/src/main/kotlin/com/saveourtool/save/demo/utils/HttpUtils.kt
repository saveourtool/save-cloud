@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.demo.utils

import com.saveourtool.save.demo.config.KubernetesConfig
import com.saveourtool.save.demo.entity.Demo
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import reactor.core.publisher.Flux
import java.nio.ByteBuffer

private const val DEFAULT_BUFFER_SIZE = 4096

/**
 * Subdomain of all save-demo-agents
 */
const val CLUSTER_DOMAIN = "cluster.local"

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
 * @param demo [Demo] entity
 * @param kubernetesSettings kubernetes configuration
 * @param request callback that receives [Url]
 * @return result of type [R]
 */
suspend fun <R> demoAgentRequestWrapper(
    urlPath: String,
    demo: Demo,
    kubernetesSettings: KubernetesConfig,
    request: suspend (Url) -> R,
): R {
    val host = addressByServiceName(demo, kubernetesSettings)
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
    kubernetesSettings.currentNamespace,
    "svc",
    CLUSTER_DOMAIN,
)
    .joinToString(".")

/**
 * Get DNS-resolvable name of pod that is controlled by Service in [KubernetesConfig.agentNamespace]
 *
 * Url is constructed like this:
 *
 * {SERVICE-NAME}.{AGENT-NAMESPACE}.svc.{CLUSTER.DOMAIN}
 *
 * @param demo [Demo] entity
 * @param kubernetesSettings kubernetes configuration
 * @return DNS-resolvable name of pod in format {SERVICE-NAME}.{AGENT-NAMESPACE}.svc.{CLUSTER.DOMAIN}
 */
fun addressByServiceName(demo: Demo, kubernetesSettings: KubernetesConfig) = listOf(
    serviceNameForDemo(demo),
    kubernetesSettings.agentNamespace,
    "svc",
    CLUSTER_DOMAIN,
)
    .joinToString(".")
