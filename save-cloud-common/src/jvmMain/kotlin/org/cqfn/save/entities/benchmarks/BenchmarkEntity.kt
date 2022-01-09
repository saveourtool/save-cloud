package org.cqfn.save.entities.benchmarks

import org.cqfn.save.entities.BaseEntity
import org.cqfn.save.entities.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated


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

fun BenchmarkEntity.toEntity() = AwesomeBenchmarks(
        this.general.name,
        this.general.category,
        this.general.tags.joinToString(","),

        this.info.language,
        this.info.license,
        this.info.scenarios_num,
        this.info.description,

        this.links.homepage,
        this.links.sources,
        this.links.documentation
)
