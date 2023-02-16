package com.saveourtool.save.demo.runners.pod

import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.service.KubernetesService
import com.saveourtool.save.utils.deferredToMono
import reactor.core.publisher.Mono

/**
 * Class that implements [PodRunner] and is a [Demo] PodRunner.
 *
 */
class DemoPodRunner(
    private val kubernetesService: KubernetesService,
    private val demo: Demo,
) : PodRunner {
    override fun getUrl(demoRunRequest: DemoRunRequest): Mono<String> = deferredToMono {
        kubernetesService.getUrl(demo)
    }
}
