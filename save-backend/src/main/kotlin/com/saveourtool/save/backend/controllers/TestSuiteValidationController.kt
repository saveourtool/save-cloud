package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.ParallelFluxResponse
import com.saveourtool.save.backend.utils.withHttpHeaders
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.test.TestSuiteValidationProgress
import com.saveourtool.save.v1
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.MediaType.APPLICATION_NDJSON_VALUE
import org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.ParallelFlux
import reactor.core.scheduler.Schedulers
import kotlin.streams.asStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Demonstrates _Server-Sent Events_ (SSE).
 */
@ApiSwaggerSupport
@RestController
@RequestMapping(path = ["/api/$v1/a"])
class TestSuiteValidationController {
    /**
     * @return a stream of events.
     */
    @GetMapping(
        path = ["/validate"],
        headers = [
            "$ACCEPT=$TEXT_EVENT_STREAM_VALUE",
            "$ACCEPT=$APPLICATION_NDJSON_VALUE",
        ],
        produces = [
            TEXT_EVENT_STREAM_VALUE,
            APPLICATION_NDJSON_VALUE,
        ],
    )
    @ApiResponse(responseCode = "406", description = "Could not find acceptable representation.")
    fun sequential(): ParallelFluxResponse<TestSuiteValidationProgress> =
            withHttpHeaders {
                overallProgress()
            }

    @Suppress("MAGIC_NUMBER")
    private fun singleCheck(
        checkId: String,
        checkName: String,
        duration: Duration,
    ): Flux<TestSuiteValidationProgress> {
        @Suppress("MagicNumber")
        val ticks = 0..100

        val delayMillis = duration.inWholeMilliseconds / (ticks.count() - 1)

        return Flux.fromStream(ticks.asSequence().asStream())
            .map { percentage ->
                TestSuiteValidationProgress(
                    checkId = checkId,
                    checkName = checkName,
                    percentage = percentage,
                )
            }
            .map { item ->
                Thread.sleep(delayMillis)
                item
            }
            .subscribeOn(Schedulers.boundedElastic())
    }

    @Suppress("MAGIC_NUMBER")
    private fun overallProgress(): ParallelFlux<TestSuiteValidationProgress> {
        @Suppress("ReactiveStreamsUnusedPublisher")
        val checks = arrayOf(
            singleCheck(
                "check A",
                "Searching for plug-ins with zero tests",
                10.seconds,
            ),

            singleCheck(
                "check B",
                "Searching for test suites with wildcard mode",
                20.seconds,
            ),

            singleCheck(
                "check C",
                "Ordering pizza from the nearest restaurant",
                30.seconds,
            ),
        )

        return when {
            checks.isEmpty() -> Flux.empty<TestSuiteValidationProgress>().parallel()

            else -> checks.reduce { left, right ->
                left.mergeWith(right)
            }
                .parallel(Runtime.getRuntime().availableProcessors())
                .runOn(Schedulers.parallel())
        }
    }
}
