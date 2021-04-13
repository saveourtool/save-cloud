package org.cqfn.save.entities

import org.cqfn.save.testsuite.TestSuiteType

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * @property type
 * @property name
 * @property project
 * @property dateAdded
 */
@Suppress("USE_DATA_CLASS")
@Entity
@Table(name = "test_suite")
class TestSuite(

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    var type: TestSuiteType,

    @Column(name = "name")
    var name: String,

    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project? = null,

    @Column(name = "date_added")
    var dateAdded: LocalDateTime

) : BaseEntity()
