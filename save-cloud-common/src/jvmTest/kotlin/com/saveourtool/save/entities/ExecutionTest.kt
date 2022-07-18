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
    fun formatAndSetTestSuiteIds() {
        execution.testSuiteIds = null

        execution.formatAndSetTestSuiteIds(emptyList())
        assertEquals("", execution.testSuiteIds)

        execution.testSuiteIds = null
        execution.formatAndSetTestSuiteIds(listOf(1L, 2L, 3L))
        assertEquals("1, 2, 3", execution.testSuiteIds)
        execution.formatAndSetTestSuiteIds(listOf(4L))
        assertEquals("4", execution.testSuiteIds)

        execution.testSuiteIds = null
        execution.formatAndSetTestSuiteIds(listOf(3L, 2L, 1L))
        assertEquals("1, 2, 3", execution.testSuiteIds)

        execution.testSuiteIds = null
        execution.formatAndSetTestSuiteIds(listOf(1L, 2L, 3L, 2L, 1L))
        assertEquals("1, 2, 3", execution.testSuiteIds)
    }

    @Test
    fun parseAndGetAdditionalFiles() {
        execution.additionalFiles = ""
        assertEquals(emptyList(), execution.parseAndGetAdditionalFiles())

        execution.additionalFiles = "file1:1;file2:2;file3:3"
        assertEquals(
            listOf(FileKey("file1", 1), FileKey("file2", 2), FileKey("file3", 3)),
            execution.parseAndGetAdditionalFiles()
        )

        execution.additionalFiles = "file3:3;file2:2;file1:1"
        assertEquals(
            listOf(FileKey("file3", 3), FileKey("file2", 2), FileKey("file1", 1)),
            execution.parseAndGetAdditionalFiles()
        )
    }

    @Test
    fun appendAdditionalFile() {
        execution.additionalFiles = ""

        execution.appendAdditionalFile(FileKey("file1", 1))
        assertEquals("file1:1", execution.additionalFiles)

        execution.appendAdditionalFile(FileKey("file3", 3))
        assertEquals("file1:1;file3:3", execution.additionalFiles)

        execution.appendAdditionalFile(FileKey("file2", 2))
        assertEquals("file1:1;file3:3;file2:2", execution.additionalFiles)
    }
}
