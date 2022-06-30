package com.saveourtool.save.entities

import com.saveourtool.save.testsuite.TestSuiteType
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.testsuite.TestSuitesSourceLocationType
import javax.persistence.Entity

/**
 * @property type
 * @property name
 * @property description
 * @property locationType
 * @property locationInfo
 */
@Entity
class TestSuitesSource(
    var type: TestSuiteType,
    var name: String = "Undefined",
    var description: String? = null,
    var locationType: TestSuitesSourceLocationType,
    var locationInfo: String,
) : BaseEntity() {
    /**
     * @return DTO entity [TestSuitesSourceDto]
     */
    fun toDto() = TestSuitesSourceDto(
        type = type,
        name = name,
        description = description,
        locationType = locationType,
        locationInfo = locationInfo,
    )
}
