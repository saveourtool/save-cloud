package com.saveourtool.save.storage

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class S3StorageUtilsKtTest {
    @Test
    fun testConcatS3Key() {
        doTestConcatS3Key("prefix", "suffix", "prefix/suffix")
        doTestConcatS3Key("prefix/", "suffix", "prefix/suffix")
        doTestConcatS3Key("prefix", "/suffix", "prefix/suffix")
        doTestConcatS3Key("prefix/", "/suffix", "prefix/suffix")
        doTestConcatS3Key("prefix", "", "prefix")
        doTestConcatS3Key("prefix/", "", "prefix")
        doTestConcatS3Key("", "suffix", "suffix")
        doTestConcatS3Key("", "/suffix", "suffix")

        Assertions.assertThrows(IllegalArgumentException::class.java) {
            concatS3Key("", "")
        }

        Assertions.assertEquals(
            "prefix/middle/suffix",
            concatS3Key("prefix", "/middle", "/suffix")
        )
    }

    private fun doTestConcatS3Key(prefix: String, suffix: String, expectedValue: String) {
        Assertions.assertEquals(
            expectedValue,
            concatS3Key(prefix, suffix)
        )
    }

    @Test
    fun testS3KeyToParts() {
        doTestConcatS3Key("", "only-prefix/", listOf("only-prefix"))
        doTestConcatS3Key("", "prefix/suffix", listOf("prefix", "suffix"))
        doTestConcatS3Key("", "prefix/middle/suffix", listOf("prefix", "middle", "suffix"))
        doTestConcatS3Key("prefix", "prefix/middle/suffix", listOf("middle", "suffix"))
        doTestConcatS3Key("prefix/", "prefix/middle/suffix", listOf("middle", "suffix"))
    }

    private fun doTestConcatS3Key(prefix: String, s3Key: String, expectedValue: List<String>) {
        Assertions.assertEquals(
            expectedValue,
            s3Key.s3KeyToPartsTill(prefix)
        )
    }
}
