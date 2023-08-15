package com.saveourtool.save.osv.processor.go

import kotlinx.serialization.Serializable

/**
 * Go Vulnerability Database `affected[].ecosystem_specific`
 *
 * @property imports
 */
@Serializable
data class GoImports(
    val imports: List<GoImport>,
) {
    companion object {
        /**
         * `affected[].ecosystem_specific.imports[]`
         *
         * @property path
         * @property symbols
         */
        @Serializable
        data class GoImport(
            val path: String,
            val symbols: List<String>,
        )
    }
}
