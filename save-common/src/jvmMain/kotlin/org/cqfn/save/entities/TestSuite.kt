package org.cqfn.save.entities

import org.cqfn.save.testsuite.TestSuiteType
import java.time.LocalDateTime
import javax.persistence.EnumType
import javax.persistence.Enumerated

/**
 * @property id
 * @property type
 * @property name
 * @property projectId
 * @property dateAdded
 */
@Entity
class TestSuite(
    @Enumerated(EnumType.STRING)
    var type: TestSuiteType,
    var name: String,
    var projectId: Long? = null,
    var dateAdded: LocalDateTime
) {
    @Id @GeneratedValue var id: Long? = null
}
