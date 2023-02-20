package com.saveourtool.save.demo.runners.pod

import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.service.KubernetesService
import io.ktor.client.statement.*
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono

/**
 * Class that implements [PodRunner] and is a [Demo] PodRunner.
 *
 */
class DemoPodRunner(
    private val kubernetesService: KubernetesService,
    private val demo: Demo,
) : PodRunner {
    override suspend fun sendRunRequest(demoRunRequest: DemoRunRequest): HttpResponse? = kubernetesService.sendRunRequest(demo, demoRunRequest)
}
