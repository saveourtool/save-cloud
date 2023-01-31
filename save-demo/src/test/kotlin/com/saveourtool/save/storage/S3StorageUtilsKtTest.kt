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
    }

    private fun doTestConcatS3Key(prefix: String, suffix: String, expectedValue: String) {
        Assertions.assertEquals(
            expectedValue,
            concatS3Key(prefix, suffix)
        )
    }
}
