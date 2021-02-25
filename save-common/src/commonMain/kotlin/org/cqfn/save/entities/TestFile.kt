package org.cqfn.save.entities

import org.cqfn.save.enums.TestStatus

@Entity
class TestFile (
    @Id
    @GeneratedValue
    var id: Int? = null,
    var testName: String,
    var status: TestStatus
)