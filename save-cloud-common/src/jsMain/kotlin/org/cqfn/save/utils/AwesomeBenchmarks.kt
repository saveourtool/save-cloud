package org.cqfn.save.utils

import kotlinx.serialization.Serializable
import org.cqfn.save.entities.benchmarks.BenchmarkCategoryEnum

@Serializable
data class AwesomeBenchmarks(
        val id: Long,
        val name: String,
        val category: BenchmarkCategoryEnum,
        val tags: String,
        val language: String,
        val license: String,
        val scenarios_num: Long,
        val description: String,
        val homepage: String,
        val sources: String,
        val documentation: String,
)



