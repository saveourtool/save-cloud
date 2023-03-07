package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.test.TestSuiteValidationError
import com.saveourtool.save.test.TestSuiteValidationProgress
import com.saveourtool.save.utils.getLogger
import org.springframework.jmx.export.annotation.ManagedOperation
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Service

@Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION"
)
@Service
@ManagedResource
class TestSuiteValidatorClient(private val service: TestSuiteValidationService) {
    @ManagedOperation
    fun validate() {
        service.validateAll(emptyList())
            .subscribe()
    }

    @ManagedOperation
    fun validateAndRequestSingle() {
        service.validateAll(emptyList())
            .doOnNext { status ->
                when (status) {
                    is TestSuiteValidationProgress -> logger.info("First status event received: ${status.percentage}%")
                    is TestSuiteValidationError -> logger.info("Error: ${status.message}")
                }
            }
            .sequential()
            .next()
            .block()
    }

    @ManagedOperation
    fun validateAndCancel() {
        service.validateAll(emptyList())
            .subscribe()
            .dispose()
        logger.info("Subscription for status update events cancelled.")
    }

    private companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<TestSuiteValidatorClient>()
    }
}
