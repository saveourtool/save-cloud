package org.cqfn.save.backend.controllers

import org.cqfn.save.entities.benchmarks.BenchmarkEntity
import org.cqfn.save.backend.service.AwesomeBenchmarksService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
class AwesomeBenchmarksController(
        private val awesomeBenchmarksService: AwesomeBenchmarksService,
) {
    private val log = LoggerFactory.getLogger(AwesomeBenchmarksController::class.java)

    @PostMapping("/internal/upload/awesome-benchmarks")
    fun uploadAwesomeBenchmarks(@RequestBody(required = true) benchmarks: List<BenchmarkEntity>) {
        log.info("Received a request to save awesome-benchmarks to the db")
        awesomeBenchmarksService.saveBenchmarksToDb(benchmarks)
        log.info("Saved requested awesome-benchmarks to the db")
    }

    @GetMapping("/api/awesome-benchmarks")
    fun getAllAwesomeBenchmarks() =
        awesomeBenchmarksService.getAllBenchmarks()
}
