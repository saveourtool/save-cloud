/**
 * This class contains util methods for Spring
 */

package com.saveourtool.save.utils

import com.saveourtool.save.storage.Storage
import org.springframework.http.codec.multipart.Part
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * upload with [Part] as content
 *
 * @param key a key for provided content
 * @param content
 * @return count of written bytes
 */
fun <K> Storage<K>.upload(key: K, content: Part): Mono<Long> = content.content()
    .map { it.asByteBuffer() }
    .let { upload(key, it) }

/**
 * upload with [Mono] of [Part] as content
 *
 * @param key a key for provided content
 * @param contentMono
 * @return count of written bytes
 */
fun <K> Storage<K>.upload(key: K, contentMono: Mono<Part>): Mono<Long> = contentMono
    .flatMap { upload(key, it) }

/**
 * overwrite with [Part] as content
 *
 * @param key a key for provided content
 * @param content
 * @return count of written bytes
 */
fun <K> Storage<K>.overwrite(key: K, content: Part): Mono<Long> = content.content()
    .map { it.asByteBuffer() }
    .let { overwrite(key, it) }

/**
 * overwrite with [Mono] of [Part] as content
 *
 * @param key a key for provided content
 * @param contentMono
 * @return count of written bytes
 */
fun <K> Storage<K>.overwrite(key: K, contentMono: Mono<Part>): Mono<Long> = contentMono
    .flatMap { overwrite(key, it) }

/**
 * @return [WebClient] with enabled Huawei proxy
 */
@Suppress(
    "COMPLEX_EXPRESSION",
    "MAGIC_NUMBER", "MagicNumber",
)
fun WebClient.enableHuaweiProxy(): WebClient = this
    .mutate()
    .clientConnector(
        org.springframework.http.client.reactive.ReactorClientHttpConnector(
            reactor.netty.http.client.HttpClient.create()
                .proxy { proxy ->
                    proxy.type(reactor.netty.transport.ProxyProvider.Proxy.HTTP)
                        .host("proxy.huawei.com")
                        .port(8080)
                }
        )
    )
    .build()
