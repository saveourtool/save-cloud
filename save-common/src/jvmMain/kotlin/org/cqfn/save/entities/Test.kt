package org.cqfn.save.entities

import java.time.LocalDateTime

@Entity
class Test (
    var expectedFilePath: String,
    var testFilePath: String,
    var dateAdded: LocalDateTime,
    var testSuiteId: Long,
    @Id var id: String
)
