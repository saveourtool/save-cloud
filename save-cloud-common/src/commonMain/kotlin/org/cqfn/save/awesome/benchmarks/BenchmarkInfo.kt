package org.cqfn.save.awesome.benchmarks

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkInfo(
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

enum class BenchmarkCategoryEnum {
    STATIC_ANALYSIS,
    CODE_STANDARD,
    PERFORMANCE,
    AUDIT,
    AI,
}
