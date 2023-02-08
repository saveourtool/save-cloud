package com.saveourtool.save.test.analysis.internal

import com.saveourtool.save.test.analysis.api.TestId
import com.saveourtool.save.test.analysis.api.TestIdGenerator
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * The default implementation of [TestIdGenerator], which uses the strongest
 * SHA-2 hash available.
 */
internal class DefaultTestIdGenerator : TestIdGenerator {
    @Suppress("TOO_MANY_PARAMETERS")
    override fun testId(
        organizationName: String,
        projectName: String,
        testSuiteSourceName: String?,
        testSuiteVersion: String?,
        testSuiteName: String?,
        pluginName: String,
        filePath: String
    ): TestId =
            TestId(
                toHash(
                    organizationName,
                    projectName,
                    testSuiteSourceName,
                    testSuiteVersion,
                    testSuiteName,
                    pluginName,
                    filePath,
                )
            )

    private companion object {
        @Suppress(
            "LongParameterList",
            "TOO_MANY_PARAMETERS",
            "AVOID_NULL_CHECKS",
        )
        private fun toHash(
            organizationName: String,
            projectName: String,
            testSuiteSourceName: String?,
            testSuiteVersion: String?,
            testSuiteName: String?,
            pluginName: String,
            filePath: String,
        ): String {
            val digest = firstDigestInstance(
                "SHA-512",
                "SHA-384",
                "SHA-256"
            )

            sequenceOf(
                organizationName,
                projectName,
                testSuiteSourceName,
                testSuiteVersion,
                testSuiteName,
                pluginName,
                filePath,
            ).forEach { value ->
                /*-
                 * TLV without the tag ("LV").
                 *
                 * Even if the "V" field is `null`, the digest is still updated
                 * with data in order to avoid collisions.
                 */
                val length = value?.length ?: -1
                digest.update(length)

                if (value != null) {
                    digest.update(value)
                }
            }

            return digest.digest().toHexString()
        }

        private fun ByteArray.toHexString(): String =
                joinToString(separator = "") { eachByte ->
                    "%02x".format(eachByte)
                }

        @Suppress(
            "MagicNumber",
            "MAGIC_NUMBER",
        )
        private fun Int.toByteArray(): ByteArray {
            val buffer = ByteArray(4)

            /*
             * Big-endian, most significant byte first.
             */
            buffer.forEachIndexed { index, _ ->
                buffer[index] = (this shr (8 * (3 - index))).toByte()
            }

            return buffer
        }

        private fun MessageDigest.update(input: Int): Unit =
                update(input.toByteArray())

        private fun MessageDigest.update(input: String): Unit =
                update(input.toByteArray())

        private fun firstDigestInstance(vararg algorithms: String): MessageDigest =
                algorithms
                    .asSequence()
                    .map { algorithm ->
                        try {
                            MessageDigest.getInstance(algorithm)
                        } catch (_: NoSuchAlgorithmException) {
                            null
                        }
                    }
                    .filterNotNull()
                    .first()
    }
}
