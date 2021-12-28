/**
 * This file contains expected annotations for jpa data
 */

@file:Suppress("MISSING_KDOC_TOP_LEVEL", "EMPTY_PRIMARY_CONSTRUCTOR")

package org.cqfn.save.entities

import org.cqfn.save.utils.EnumType

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class Entity()

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class Id()

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class GeneratedValue()

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class JoinColumn(
    val name: String,
    val referencedColumnName: String,
    val unique: Boolean,
    val nullable: Boolean,
    val insertable: Boolean,
    val updatable: Boolean,
    val columnDefinition: String,
    val table: String,
    val foreignKey: ForeignKey,
)

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class ManyToOne()

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class OneToMany()

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class MappedSuperclass()

/**
 * @property value
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class Enumerated(val value: EnumType)

@OptIn(ExperimentalMultiplatform::class)
expect annotation class ForeignKey()