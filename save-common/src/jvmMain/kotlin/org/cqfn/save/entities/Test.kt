package org.cqfn.save.entities

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property filePath
 * @property dateAdded
 * @property testSuite
 */
@Entity
class Test(

    var filePath: String,

    var dateAdded: LocalDateTime,

    @ManyToOne
    @JoinColumn(name = "test_suite_id")
    var testSuite: TestSuite,

) : BaseEntity()
