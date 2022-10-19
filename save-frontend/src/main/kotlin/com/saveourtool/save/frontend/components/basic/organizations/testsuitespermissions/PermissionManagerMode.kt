package com.saveourtool.save.frontend.components.basic.organizations.testsuitespermissions

/**
 * Enum class that defines current state of [manageTestSuitePermissionsComponent] (mostly state of the modal inside component)
 */
internal enum class PermissionManagerMode {
    /**
     * State when a modal with three input forms is shown: what, where and how to add.
     */
    MANAGE,

    /**
     * State when success (or error) message is shown.
     */
    MESSAGE,

    /**
     * Make test suites public or private
     */
    PUBLISH,

    /**
     * Select test suites that should be managed in case of visibility.
     */
    SUITE_SELECTOR_FOR_PUBLISH,

    /**
     * Select test suites that should be managed in case of rights.
     */
    SUITE_SELECTOR_FOR_RIGHTS,
    ;
}
