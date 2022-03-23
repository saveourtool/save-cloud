/**
 * Controller for processing Users and their roles:
 * 1) to put new roles of users
 * 2) to get users and their roles by project
 */

package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.AwesomeBenchmarksService
import org.cqfn.save.backend.service.LnkUserProjectService
import org.cqfn.save.entities.benchmarks.BenchmarkEntity

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
/**
 * Controller for processing awesome-benchmarks
 */
class LnkUserProjectController(
    private val lnkUserProjectService: LnkUserProjectService,
) {
    private val log = LoggerFactory.getLogger(LnkUserProjectController::class.java)
//
//    /**
//     * @param benchmarks
//     */
//    @PostMapping("/internal/upload/awesome-benchmarks")
//    fun uploadAwesomeBenchmarks(@RequestBody(required = true) benchmarks: List<BenchmarkEntity>) {
//        log.info("Received a request to save awesome-benchmarks to the db")
//        awesomeBenchmarksService.saveBenchmarksToDb(benchmarks)
//        log.info("Saved requested awesome-benchmarks to the db")
//    }
//
//    /**
//     * @return all benchmarks from backend to frontend
//     */
//    @GetMapping("/api/awesome-benchmarks")
//    fun getAllAwesomeBenchmarks() =
//            awesomeBenchmarksService.getAllBenchmarks()
}
