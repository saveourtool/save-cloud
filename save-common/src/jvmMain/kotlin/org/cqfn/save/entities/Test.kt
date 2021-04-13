package org.cqfn.save.entities

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * @property expectedFilePath
 * @property testFilePath
 * @property dateAdded
 * @property testSuite
 */
@Entity
@Table(name = "test")
class Test(

    @Column(name = "expected_file_path")
    var expectedFilePath: String,

    @Column(name = "test_file_path")
    var testFilePath: String,

    @Column(name = "date_added")
    var dateAdded: LocalDateTime,

    @ManyToOne
    @JoinColumn(name = "test_suite_id")
    var testSuite: TestSuite,

) : BaseEntity()
