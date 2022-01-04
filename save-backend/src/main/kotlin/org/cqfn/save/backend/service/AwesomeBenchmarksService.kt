package org.cqfn.save.backend.service

import org.cqfn.save.backend.controllers.AwesomeBenchmarksController
import org.cqfn.save.backend.repository.AwesomeBenchmarksRepository
import org.cqfn.save.entities.benchmarks.AwesomeBenchmarks
import org.cqfn.save.entities.benchmarks.BenchmarkEntity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AwesomeBenchmarksService {
    @Autowired
    private lateinit var awesomeBenchmarksRepository: AwesomeBenchmarksRepository
    private val log = LoggerFactory.getLogger(AwesomeBenchmarksService::class.java)

    @Transactional
    internal fun saveBenchmarksToDb(benchmarks: List<BenchmarkEntity>) {
        log.info("Saving Awesome Benchmarks to 'awesome_benchmarks' table in DB: $benchmarks")
        // as we plan to override everything, we can simple delete all records in the table
        awesomeBenchmarksRepository.deleteAll()
        // flush is always needed after the deletion
        awesomeBenchmarksRepository.flush()
        awesomeBenchmarksRepository.saveAll(benchmarks.map { it.toAwesomeBenchmarksEntity() })
    }

    internal fun getAllBenchmarks(): List<AwesomeBenchmarks> = awesomeBenchmarksRepository.findAll()
}
