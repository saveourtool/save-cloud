package org.cqfn.save.entities

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property hash
 * @property filePath path to this test relative to the project root
 * @property dateAdded
 * @property testSuite
 * @property pluginName name of a plugin which this test belongs to
 * @property tags list of tags of current test
 */
@Entity
class Test(

    var hash: String,

    var filePath: String,

    var pluginName: String,

    var dateAdded: LocalDateTime,

    @ManyToOne
    @JoinColumn(name = "test_suite_id")
    var testSuite: TestSuite,

    var tags: String?,

) : BaseEntity()
