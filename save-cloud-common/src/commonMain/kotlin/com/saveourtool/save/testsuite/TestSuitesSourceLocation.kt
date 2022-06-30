package com.saveourtool.save.testsuite

/**
 * Base interface for location to [com.saveourtool.save.entities.TestSuitesSource]
 */
interface TestSuitesSourceLocation {
    /**
     * @return type of this location
     */
    fun getType(): TestSuitesSourceLocationType

    /**
     * @return formatted String to store it in database
     */
    fun formatForDatabase(): String

    /**
     * Base interface for companions
     */
    interface Companion<T : TestSuitesSourceLocation> {
        /**
         * @param databaseValue
         * @return parsed value taken from database
         */
        fun parseFromDatabase(databaseValue: String): T
    }
}
