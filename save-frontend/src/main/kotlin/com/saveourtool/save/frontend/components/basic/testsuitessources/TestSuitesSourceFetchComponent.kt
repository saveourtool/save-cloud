package com.saveourtool.save.frontend.components.basic.testsuitessources

import com.saveourtool.save.testsuite.TestSuitesSourceDto
import react.FC
import react.Props

/**
 * Properties for [testSuitesSourceFetchComponent]
 */
external interface TestSuitesSourceFetchComponent : Props {
    /**
     * TestSuitesSource to be fetched
     */
    var testSuitesSourceDto: TestSuitesSourceDto
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun testSuitesSourceFetchComponent() = FC<TestSuitesSourceFetchComponent> { props ->

}
