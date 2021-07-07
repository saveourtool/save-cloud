package org.cqfn.save.domain

/**
 * Enum of project save status
 */
@Serializable
@Suppress("CUSTOM_GETTERS_SETTERS")
enum class ProjectSaveStatus {
    /**
     * Project exist
     */
    EXIST {
        override val message: String
            get() = "Project already exist"
    },

    /**
     * New project
     */
    NEW {
        override val message: String
            get() = "Project saved successfully"
    },
    ;

    /**
     * Message to front
     */
    abstract val message: String
}
