package org.cqfn.save.entities.benchmarks

import kotlinx.serialization.Serializable
import org.cqfn.save.entities.BaseEntity
import org.cqfn.save.entities.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Serializable
data class BenchmarkEntity(
        val general: General,
        val info: Info,
        val links: Links,
) {
    fun toAwesomeBenchmarksEntity() = AwesomeBenchmarks(
            general.name,
            general.category,
            general.tags.joinToString(","),

            info.language,
            info.license,
            info.scenarios_num,
            info.description,

            links.homepage,
            links.sources,
            links.documentation
    )
}

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

@Serializable
enum class BenchmarkCategoryEnum {
    STATIC_ANALYSIS,
    CODE_STANDARD,
    PERFORMANCE,
    AUDIT,
    AI,
}

@Entity
data class AwesomeBenchmarks(
        val name: String,
        @Enumerated(EnumType.STRING)
        val category: BenchmarkCategoryEnum,
        val tags: String,
        val language: String,
        val license: String,
        val scenarios_num: Long,
        val description: String,
        val homepage: String,
        val sources: String,
        val documentation: String,
): BaseEntity()
