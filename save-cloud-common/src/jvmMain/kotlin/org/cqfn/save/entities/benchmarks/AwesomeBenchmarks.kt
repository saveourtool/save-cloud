package com.saveourtool.save.entities.benchmarks

import com.saveourtool.save.entities.BaseEntity
import com.saveourtool.save.entities.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated

/**
 * Enity that is used for representing the entity in the DB
 *
 * @property name
 * @property category
 * @property tags
 * @property language
 * @property license
 * @property scenarios_num
 * @property description
 * @property homepage
 * @property sources
 * @property documentation
 */
@Entity
data class AwesomeBenchmarks(
    val name: String,
    @Enumerated(EnumType.STRING)
    val category: BenchmarkCategoryEnum,
    val tags: String,
    val language: String,
    val license: String,
    @Suppress("VARIABLE_NAME_INCORRECT_FORMAT", "ConstructorParameterNaming")
    val scenarios_num: Long,
    val description: String,
    val homepage: String,
    val sources: String,
    val documentation: String,
) : BaseEntity()

/**
 * @return the benchmark converted to entity (in db format)
 */
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
