package com.saveourtool.save.orchestrator.service

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Instant

class DockerAgentLogService(
    private val dockerClient: DockerClient,
) : AgentLogService {
    override fun get(containerName: String, from: Instant, to: Instant): Mono<List<String>> {
        val callback = LogContainerResultCallback()
        dockerClient.logContainerCmd(containerName)
            .exec(callback)
            .awaitCompletion()
        return callback.getResult().toMono()
    }
}

internal class LogContainerResultCallback : ResultCallback.Adapter<Frame>() {
    private val logs: MutableList<String> = mutableListOf()

    override fun onNext(frame: Frame?) {
        frame?.let { logs.add(it.toString()) }
    }

    fun getResult(): List<String> = logs
}