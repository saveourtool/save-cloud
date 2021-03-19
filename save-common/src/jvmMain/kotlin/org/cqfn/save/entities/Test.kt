package org.cqfn.save.entities

import java.time.LocalDateTime

/**
 * @property expectedFilePath
 * @property testFilePath
 * @property dateAdded
 * @property testSuiteId
 * @property id
 */
@Entity
class Test(
    var expectedFilePath: String,
    var testFilePath: String,
    var dateAdded: LocalDateTime,
    var testSuiteId: Long,
    @Id var id: String
)
