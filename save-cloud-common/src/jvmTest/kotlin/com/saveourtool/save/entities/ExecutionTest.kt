@file:Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
)

package com.saveourtool.save.entities

import com.saveourtool.save.domain.FileKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ExecutionTest {
    private val project = Project(
        name = "projectName",
        url = null,
        description = null,
        status = ProjectStatus.CREATED,
        organization = Organization.stub(null).apply { name = "organizationName" },
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

        execution.formatAndSetTestSuiteIds(emptyList())
        assertEquals("", execution.testSuiteIds)

        execution.testSuiteIds = null
        execution.formatAndSetTestSuiteIds(listOf(1L, 2L, 3L))
        assertEquals("1,2,3", execution.testSuiteIds)
        execution.formatAndSetTestSuiteIds(listOf(4L))
        assertEquals("4", execution.testSuiteIds)

        execution.testSuiteIds = null
        execution.formatAndSetTestSuiteIds(listOf(3L, 2L, 1L))
        assertEquals("1,2,3", execution.testSuiteIds)

        execution.testSuiteIds = null
        execution.formatAndSetTestSuiteIds(listOf(1L, 2L, 3L, 2L, 1L))
        assertEquals("1,2,3", execution.testSuiteIds)
    }

    @Test
    fun parseAndGetAdditionalFiles() {
        execution.additionalFiles = ""
        assertEquals(emptyList(), execution.getFileKeys())

        execution.additionalFiles = "file1:1;file2:2;file3:3"
        assertEquals(
            listOf(
                FileKey(execution.project.toProjectCoordinates(), "file1", 1),
                FileKey(execution.project.toProjectCoordinates(), "file2", 2),
                FileKey(execution.project.toProjectCoordinates(), "file3", 3)
            ),
            execution.getFileKeys()
        )

        execution.additionalFiles = "file3:3;file2:2;file1:1"
        assertEquals(
            listOf(
                FileKey(execution.project.toProjectCoordinates(), "file3", 3),
                FileKey(execution.project.toProjectCoordinates(), "file2", 2),
                FileKey(execution.project.toProjectCoordinates(), "file1", 1)
            ),
            execution.getFileKeys()
        )
    }
}
