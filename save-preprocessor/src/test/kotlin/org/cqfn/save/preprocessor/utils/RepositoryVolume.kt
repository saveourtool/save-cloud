package org.cqfn.save.preprocessor.utils

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

/**
 * Override path to download repository
 */
open class RepositoryVolume {
    companion object {
        @OptIn(ExperimentalPathApi::class)
        private val volume: String by lazy {
            createTempDirectory("repositories").toAbsolutePath().toString()
        }

        /**
         * @param registry
         */
        @DynamicPropertySource
        @JvmStatic
        fun dbProperties(registry: DynamicPropertyRegistry) {
            registry.add("save.repository") { volume }
        }
    }
}
