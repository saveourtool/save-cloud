@file:Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
)

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
        organization = Organization.stub(null),
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
    fun appendTestSuiteIds() {
        execution.testSuiteIds = null

        execution.appendTestSuiteIds(emptyList())
        assertEquals("", execution.testSuiteIds)

        execution.testSuiteIds = null
        execution.appendTestSuiteIds(listOf(1L, 2L, 3L))
        assertEquals("1, 2, 3", execution.testSuiteIds)
        execution.appendTestSuiteIds(listOf(4L))
        assertEquals("1, 2, 3, 4", execution.testSuiteIds)

        execution.testSuiteIds = null
        execution.appendTestSuiteIds(listOf(4L))
        assertEquals("4", execution.testSuiteIds)
        execution.appendTestSuiteIds(listOf(3L, 2L, 1L))
        assertEquals("1, 2, 3, 4", execution.testSuiteIds)

        execution.testSuiteIds = null
        execution.appendTestSuiteIds(listOf(2L, 3L))
        assertEquals("2, 3", execution.testSuiteIds)
        execution.appendTestSuiteIds(listOf(2L, 1L))
        assertEquals("1, 2, 3", execution.testSuiteIds)

        execution.testSuiteIds = null
        execution.appendTestSuiteIds(listOf(1L, 2L, 3L, 2L, 1L))
        assertEquals("1, 2, 3", execution.testSuiteIds)
    }

    @Test
    fun parseAndGetAdditionalFiles() {
        execution.additionalFiles = null
        assertNull(execution.parseAndGetAdditionalFiles())

        execution.additionalFiles = "file1;file2;file3"
        assertNotNull(execution.parseAndGetAdditionalFiles()) {
            assertEquals(listOf("file1", "file2", "file3"), it)
        }

        execution.additionalFiles = "file3;file2;file1"
        assertNotNull(execution.parseAndGetAdditionalFiles()) {
            assertEquals(listOf("file3", "file2", "file1"), it)
        }
    }

    @Test
    fun appendAdditionalFile() {
        execution.additionalFiles = null

        execution.appendAdditionalFile("file1")
        assertEquals("file1", execution.additionalFiles)

        execution.appendAdditionalFile("file3")
        assertEquals("file1;file3", execution.additionalFiles)

        execution.appendAdditionalFile("file2")
        assertEquals("file1;file3;file2", execution.additionalFiles)
    }
}
