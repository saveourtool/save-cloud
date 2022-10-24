package com.saveourtool.save.orchestrator.service

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import org.springframework.context.annotation.Profile
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Instant
import java.util.concurrent.CompletableFuture

/**
 * @property dockerClient a client to docker
 */
@Profile("dev")
class DockerAgentLogService(
    private val dockerClient: DockerClient,
) : AgentLogService {
    override fun get(containerName: String, from: Instant, to: Instant): Mono<List<String>> {
        val logs = mutableListOf<String>()
        val callback = object : ResultCallback.Adapter<Frame>() {
            override fun onNext(frame: Frame?) {
                frame?.let { logs.add(it.toString()) }
            }
        }
        dockerClient.logContainerCmd(containerName)
            .withStdOut(true)
            .withStdErr(true)
            .withSince(from.epochSecond.toInt())
            .withUntil(to.epochSecond.toInt())
            .exec(callback)
            .awaitCompletion()
        return logs.toMono()
    }
}