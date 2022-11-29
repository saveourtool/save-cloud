package com.saveourtool.save.test.analysis.api

/**
 * A test executed within a particular project (the same test executed in a
 * different project even within the same organization will have a different
 * [hash]).
 *
 * To create a [TestId] instance, use [TestIdGenerator.testId].
 *
 * @property hash the SHA hash that uniquely identifies this test.
 */
@JvmInline
value class TestId internal constructor(private val hash: String)
