package com.saveourtool.save.test.analysis.api.results

import com.saveourtool.save.test.analysis.algorithms.Algorithm
import com.saveourtool.save.test.analysis.api.TestAnalysisService

/**
 * The analysis result returned by [Algorithm] or [TestAnalysisService.analyze].
 *
 * @see Algorithm
 * @see TestAnalysisService.analyze
 */
sealed interface AnalysisResult
