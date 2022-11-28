package com.saveourtool.save.orchestrator.service

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Instant

/**
 * @property dockerClient a client to docker
 */
class DockerLogService(
    private val dockerClient: DockerClient,
) : ContainerLogService {
    override fun get(containerName: String, from: Instant, to: Instant): Mono<List<String>> {
        val logs: MutableList<String> = mutableListOf()
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
