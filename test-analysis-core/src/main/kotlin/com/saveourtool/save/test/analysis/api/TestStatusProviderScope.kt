package com.saveourtool.save.test.analysis.api

/**
 * Inheritors of this interface will have a [TestStatusProvider] instance in
 * their scope.
 *
 * @see TestStatusProvider
 */
interface TestStatusProviderScope<T : Enum<T>> {
    /**
     * The test status provider.
     */
    val testStatusProvider: TestStatusProvider<T>
}
