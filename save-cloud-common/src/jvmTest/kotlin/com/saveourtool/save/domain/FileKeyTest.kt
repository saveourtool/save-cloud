package com.saveourtool.save.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

internal class FileKeyTest {
    @Test
    fun format() {
        assertEquals("", emptyList<FileKey>().format())

        assertEquals(
            "org1:prj1:file1:1;org2:prj2:file2:2;org3:prj3:file3:3",
            listOf(
                FileKey(ProjectCoordinates("org1", "prj1"), "file1", 1),
                FileKey(ProjectCoordinates("org2", "prj2"), "file2", 2),
                FileKey(ProjectCoordinates("org3", "prj3"), "file3", 3),
            ).format()
        )
        assertEquals(
            "org3:prj3:file3:3;org2:prj2:file2:2;org1:prj1:file1:1",
            listOf(
                FileKey(ProjectCoordinates("org3", "prj3"), "file3", 3),
                FileKey(ProjectCoordinates("org2", "prj2"), "file2", 2),
                FileKey(ProjectCoordinates("org1", "prj1"), "file1", 1),
            ).format()
        )
    }

    @Test
    fun formatForExecution() {
        assertEquals("", emptyList<FileKey>().formatForExecution())

        assertEquals(
            "file1:1;file2:2;file3:3",
            listOf(
                FileKey(ProjectCoordinates("org", "prj"), "file1", 1),
                FileKey(ProjectCoordinates("org", "prj"), "file2", 2),
                FileKey(ProjectCoordinates("org", "prj"), "file3", 3),
            ).formatForExecution()
        )
        assertEquals(
            "file3:3;file2:2;file1:1",
            listOf(
                FileKey(ProjectCoordinates("org", "prj"), "file3", 3),
                FileKey(ProjectCoordinates("org", "prj"), "file2", 2),
                FileKey(ProjectCoordinates("org", "prj"), "file1", 1),
            ).formatForExecution()
        )

        assertThrows<IllegalArgumentException> {
            listOf(
                FileKey(ProjectCoordinates("org1", "prj1"), "file1", 1),
                FileKey(ProjectCoordinates("org2", "prj2"), "file2", 2),
                FileKey(ProjectCoordinates("org3", "prj3"), "file3", 3),
            ).formatForExecution()
        }
    }

    @Test
    fun toFileKeyList() {
        assertEquals(emptyList(), "".toFileKeyList())

        assertEquals(
            listOf(
                FileKey(ProjectCoordinates("org1", "prj1"), "file1", 1),
                FileKey(ProjectCoordinates("org2", "prj2"), "file2", 2),
                FileKey(ProjectCoordinates("org3", "prj3"), "file3", 3),
            ),
            "org1:prj1:file1:1;org2:prj2:file2:2;org3:prj3:file3:3".toFileKeyList()
        )
        assertEquals(
            listOf(
                FileKey(ProjectCoordinates("org3", "prj3"), "file3", 3),
                FileKey(ProjectCoordinates("org2", "prj2"), "file2", 2),
                FileKey(ProjectCoordinates("org1", "prj1"), "file1", 1),
            ),
            "org3:prj3:file3:3;org2:prj2:file2:2;org1:prj1:file1:1".toFileKeyList()
        )
    }

    @Test
    fun toFileKeyListWithProjectCoordinates() {
        val projectCoordinates = ProjectCoordinates("org", "prj")
        assertEquals(emptyList(), "".toFileKeyList(projectCoordinates))

        assertEquals(
            listOf(
                FileKey(ProjectCoordinates("org", "prj"), "file1", 1),
                FileKey(ProjectCoordinates("org", "prj"), "file2", 2),
                FileKey(ProjectCoordinates("org", "prj"), "file3", 3),
            ),
            "file1:1;file2:2;file3:3".toFileKeyList(projectCoordinates)
        )
        assertEquals(
            listOf(
                FileKey(ProjectCoordinates("org", "prj"), "file3", 3),
                FileKey(ProjectCoordinates("org", "prj"), "file2", 2),
                FileKey(ProjectCoordinates("org", "prj"), "file1", 1),
            ),
            "file3:3;file2:2;file1:1".toFileKeyList(projectCoordinates)
        )
    }
}
