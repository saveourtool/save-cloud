package com.saveourtool.save.demo.runners

import com.saveourtool.save.demo.entity.Demo

/**
 * Base interface for factory to create [Runner] depending on requested [RunnerType]
 */
interface RunnerFactory {
    /**
     * @param demo demo entity
     * @param version version that should be used for demo run
     * @param type
     * @return [Runner] created for provided values
     */
    fun create(demo: Demo, version: String, type: RunnerType): Runner

    /**
     * Enum that represents the type of runner that [RunnerFactory] should create
     */
    enum class RunnerType {
        /**
         * Runner that executes [Demo] in another process
         */
        CLI,

        /**
         * Runner that executes [Demo] in another pod
         */
        POD,

        /**
         * Runner that executes [Demo] in the same process as save-demo
         *
         * **Should not be used, as may lower save-demo performance**
         */
        PURE,
        ;
    }
}
