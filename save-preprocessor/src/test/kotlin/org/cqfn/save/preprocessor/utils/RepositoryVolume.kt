package org.cqfn.save.preprocessor.utils

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.io.path.createTempDirectory

/**
 * Override path to download repository
 */
interface RepositoryVolume {
    companion object {
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
