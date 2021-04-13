/**
 * This file contains expected annotations for jpa data
 */

@file:Suppress("MISSING_KDOC_TOP_LEVEL", "EMPTY_PRIMARY_CONSTRUCTOR")

package org.cqfn.save.entities

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
expect annotation class JoinColumn()

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class ManyToOne()

@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
expect annotation class OneToMany()
