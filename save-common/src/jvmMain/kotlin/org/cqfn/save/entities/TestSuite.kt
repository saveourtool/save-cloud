package org.cqfn.save.entities

import org.cqfn.save.testsuite.TestSuiteType

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property type
 * @property name
 * @property project
 * @property dateAdded
 */
@Suppress("USE_DATA_CLASS")
@Entity
class TestSuite(

    @Enumerated(EnumType.STRING)
    var type: TestSuiteType? = null,

    var name: String = "FB",

    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project? = null,

    var dateAdded: LocalDateTime? = null

) : BaseEntity()
