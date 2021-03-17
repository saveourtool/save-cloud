package org.cqfn.save.entities

import org.cqfn.save.testsuite.TestSuiteType
import java.time.LocalDateTime
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Entity
class TestSuite (
    @Id @GeneratedValue var id: Long,
    @Enumerated(EnumType.STRING)
    var type: TestSuiteType,
    var name: String,
    var projectId: Long? = null,
    var dateAdded: LocalDateTime
)
