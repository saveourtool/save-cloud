package com.saveourtool.save.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class FileKeyTest {
    @Test
    fun format() {
        assertEquals("", emptyList<FileKey>().format())

        assertEquals(
            "file1:1;file2:2;file3:3",
            listOf(FileKey("file1", 1), FileKey("file2", 2), FileKey("file3", 3))
                .format()
        )
        assertEquals(
            "file3:3;file2:2;file1:1",
            listOf(FileKey("file3", 3), FileKey("file2", 2), FileKey("file1", 1))
                .format()
        )
    }


    @Test
    fun parseList() {
        assertEquals(emptyList(), FileKey.parseList(""))

        assertEquals(
            listOf(FileKey("file1", 1), FileKey("file2", 2), FileKey("file3", 3)),
            FileKey.parseList("file1:1;file2:2;file3:3")
        )
        assertEquals(
            listOf(FileKey("file3", 3), FileKey("file2", 2), FileKey("file1", 1)),
            FileKey.parseList("file3:3;file2:2;file1:1")
        )
    }
}