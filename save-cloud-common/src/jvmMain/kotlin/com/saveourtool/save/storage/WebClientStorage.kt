package com.saveourtool.save.storage

import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.http.HttpResponse.BodyHandlers
import java.nio.ByteBuffer
import kotlin.reflect.KClass

class WebClientStorage<K : Any>(
    private val keyClass: KClass<K>,
    private val webClient: WebClient,
) : Storage<K> {
    override fun list(): Flux<K> {
        return webClient.get()
            .uri("/list")
            .retrieve()
            .bodyToFlux(keyClass.java)
    }

    override fun download(key: K): Flux<ByteBuffer> {
        return webClient.post()
            .uri("/download")
            .bodyValue(key)
            .retrieve()
            .bodyToFlux()
    }

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> {
        return webClient.post()
            .uri("/upload")
            .body(BodyInserters.fromMultipartData("key", key)
                .withPublisher("content", content, ByteBuffer::class.java))
            .retrieve()
            .bodyToMono()
    }

    override fun delete(key: K): Mono<Boolean> {
        return webClient.post()
            .uri("/delete")
            .bodyValue(key)
            .retrieve()
            .bodyToMono()
    }

    override fun contentSize(key: K): Mono<Long> {
        return webClient.post()
            .uri("/delete")
            .bodyValue(key)
            .retrieve()
            .bodyToMono()
    }

    override fun doesExist(key: K): Mono<Boolean> {
        return webClient.post()
            .uri("/check")
            .bodyValue(key)
            .retrieve()
            .bodyToMono()
    }
}