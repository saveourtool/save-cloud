package org.cqfn.save.preprocessor.utils

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Override path to download repository
 */
open class RepositoryVolume {
    companion object {
        /**
         * @param registry
         */
        @DynamicPropertySource
        @JvmStatic
        fun dbProperties(registry: DynamicPropertyRegistry) {
            registry.add("save.repository") { "../save-preprocessor/build" }
        }
    }
}
