package com.saveourtool.save.entities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ExecutionTest {

    private val project = Project(
        name = "stub",
        url = null,
        description = null,
        status = ProjectStatus.CREATED,
        organization = Organization.stub(null)
    )

    private val execution = Execution.stub(project)

    @Test
    fun parseAndGetTestSuiteIds() {
        execution.testSuiteIds = null
        assertNull(execution.parseAndGetTestSuiteIds())

        execution.testSuiteIds = "1, 2, 3"
        assertNotNull(execution.parseAndGetTestSuiteIds()) {
            assertEquals(listOf(1L, 2L, 3L), it)
        }

        execution.testSuiteIds = "3, 2, 1"
        assertNotNull(execution.parseAndGetTestSuiteIds()) {
            assertEquals(listOf(3L, 2L, 1L), it)
        }
    }

    @Test
    fun formatAndSetTestSuiteIds() {
        execution.testSuiteIds = null
        execution.formatAndSetTestSuiteIds(listOf())
        assertEquals("", execution.testSuiteIds)

        execution.testSuiteIds = null
        execution.formatAndSetTestSuiteIds(listOf(1L, 2L, 3L))
        assertEquals("1, 2, 3", execution.testSuiteIds)

        execution.testSuiteIds = null
        execution.formatAndSetTestSuiteIds(listOf(3L, 2L, 1L))
        assertEquals("1, 2, 3", execution.testSuiteIds)
    }
}