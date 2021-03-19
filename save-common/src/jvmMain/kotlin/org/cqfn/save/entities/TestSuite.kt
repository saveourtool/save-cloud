package org.cqfn.save.entities

import org.cqfn.save.testsuite.TestSuiteType
import java.time.LocalDateTime
import javax.persistence.EnumType
import javax.persistence.Enumerated

/**
 * @property type
 * @property name
 * @property projectId
 * @property dateAdded
 */
@Suppress("USE_DATA_CLASS")
@Entity
class TestSuite(
    @Enumerated(EnumType.STRING)
    var type: TestSuiteType,
    var name: String,
    var projectId: Long? = null,
    var dateAdded: LocalDateTime
) {
    /**
     * id
     */
    @Id @GeneratedValue var id: Long? = null
}
