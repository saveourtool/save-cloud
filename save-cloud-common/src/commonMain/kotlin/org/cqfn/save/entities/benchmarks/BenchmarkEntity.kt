package org.cqfn.save.entities.benchmarks

import kotlinx.serialization.Serializable

@Serializable
enum class BenchmarkCategoryEnum {
    STATIC_ANALYSIS,
    CODING_STANDARD,
    PERFORMANCE,
    AUDIT,
    AI,
}

@Serializable
data class BenchmarkEntity(
        val general: General,
        val info: Info,
        val links: Links,
)

@Serializable
data class General(
        val name: String,
        val category: BenchmarkCategoryEnum,
        val tags: ArrayList<String>,
)

@Serializable
data class Info(
        val language: String,
        val license: String,
        val scenarios_num: Long,
        val description: String,
)

@Serializable
data class Links(
        val homepage: String,
        val sources: String,
        val documentation: String,
)
