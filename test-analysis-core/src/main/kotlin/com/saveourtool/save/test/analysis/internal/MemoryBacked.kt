package com.saveourtool.save.test.analysis.internal

import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.test.analysis.api.TestId
import com.saveourtool.save.test.analysis.api.TestRun
import com.saveourtool.save.test.analysis.api.TestRuns
import com.saveourtool.save.test.analysis.api.TestStatisticsStorage
import com.saveourtool.save.test.analysis.api.TestStatusProvider
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap

/**
 * The memory-backed [TestStatisticsStorage] implementation.
 *
 * @property testStatusProvider the converter from the implementation-specific
 *   test status into boolean success/failure flags.
 * @property slidingWindowSize the size of the sliding window (the maximum sample
 *   size preserved in memory for any given test).
 * @see TestStatisticsStorage
 */
class MemoryBacked(
    override val testStatusProvider: TestStatusProvider<TestResultStatus> = TestStatusProvider(),
    private val slidingWindowSize: Int = DEFAULT_SLIDING_WINDOW_SIZE,
) : MutableTestStatisticsStorage {
    private val groupedTestRuns: MutableMap<TestId, MutableTestRuns> = ConcurrentHashMap()

    override fun getExecutionStatistics(
        id: TestId,
        ignoreIndeterminate: Boolean,
    ): TestRuns =
            with(testStatusProvider) {
                val testRuns = groupedTestRuns[id].orEmpty()

                when {
                    ignoreIndeterminate -> testRuns.asSequence().filterNot { testRun ->
                        testRun.isIndeterminate()
                    }.toList()

                    else -> testRuns
                }
            }

    override fun updateExecutionStatistics(id: TestId, testRun: TestRun) {
        groupedTestRuns.compute(id) { _, oldValue: MutableTestRuns? ->
            /*-
             * Create a copy instead of modifying the existing list:
             *
             * 1. For the mutation to be idempotent (not an issue with CHM,
             *    but definitely an issue with CSLM).
             * 2. To avoid a `ConcurrentModificationException` (a reader thread
             *    may be currently iterating over this very list). If the list
             *    is being resized, because the state may be only partially
             *    visible, an NPE (not only a CME) may get thrown on the reader
             *    thread.
             *
             * This is a poor-man's COWAL implementation (where COWAL is not
             * actually necessary), since we won't be able to atomically
             * add-and-evict anyway.
             *
             * Consider rewriting using `EvictingQueue` (Guava) or
             * `CircularFifoQueue` (Apache commons-collections).
             */
            LinkedList(oldValue.orEmpty()).apply {
                /*
                 * A naïve implementation which expects that `add(Int, TestRun)`
                 * is never invoked.
                 */
                val isAdded = add(testRun)
                while (isAdded && size > slidingWindowSize) {
                    remove()
                }
            }
        }
    }

    override fun clear() =
            groupedTestRuns.clear()

    private companion object {
        private const val DEFAULT_SLIDING_WINDOW_SIZE = Int.MAX_VALUE
    }
}
