package com.saveourtool.save.orchestrator.docker

import com.saveourtool.save.orchestrator.config.ConfigProperties

import com.github.dockerjava.api.DockerClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServerExtension
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@ExtendWith(SpringExtension::class, KubernetesMockServerExtension::class)
@EnableConfigurationProperties(ConfigProperties::class)
@EnableKubernetesMockClient
@TestPropertySource("classpath:application.properties")
class KubernetesManagerTest {
    private val dockerClient: DockerClient = mock()
    private val kubernetesManager: KubernetesManager = KubernetesManager(
        dockerClient,
        kubernetesClient,
        configProperties,
        CompositeMeterRegistry(),
    )
    @Autowired private lateinit var configProperties: ConfigProperties

    @Test
    fun `should delete a Job when stop is requested`() {
        kubernetesMockServer.expect()
            .delete()
            .withPath("/apis/batch/v1/namespaces/test/jobs/save-execution-1")
            .andReturn(HttpStatus.OK.value(), null)
            .once()

        val disposable = Mono.fromCallable {
            kubernetesMockServer.takeRequest()
        }
            .subscribeOn(Schedulers.single())
            .doOnNext {
                println(it)
                Assertions.assertNotNull(it)
            }
            .subscribe()

        kubernetesManager.stop(1)

        Assertions.assertTrue(disposable.isDisposed)
    }

    companion object {
        @JvmStatic internal lateinit var kubernetesClient: KubernetesClient
        @JvmStatic internal lateinit var kubernetesMockServer: KubernetesMockServer
    }
}
