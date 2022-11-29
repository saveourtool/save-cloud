package com.saveourtool.save.test.analysis.algorithms

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * @see FlipRateAnalysis
 */
class FlipRateAnalysisTest {
    @Test
    fun `minimum run count of zero should result in a failure`() {
        assertThatThrownBy {
            FlipRateAnalysis(minimumRunCount = 0, 0.5)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Minimum run count should be positive: 0")
    }

    @Test
    fun `negative minimum run count should result in a failure`() {
        assertThatThrownBy {
            FlipRateAnalysis(minimumRunCount = -1, 0.5)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Minimum run count should be positive: -1")
    }

    @Test
    fun `flip rate threshold of 0 should result in a failure`() {
        assertThatThrownBy {
            FlipRateAnalysis(30, flipRateThreshold = 0.0)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Flip Rate threshold should be in range (0.0, 1.0): 0.0")
    }

    @Test
    fun `flip rate threshold of 1 should result in a failure`() {
        assertThatThrownBy {
            FlipRateAnalysis(30, flipRateThreshold = 1.0)
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Flip Rate threshold should be in range (0.0, 1.0): 1.0")
    }
}
