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
