package com.saveourtool.save.entities

import com.saveourtool.save.entities.contest.ContestSampleFieldDto
import com.saveourtool.save.entities.contest.ContestSampleFieldType
import com.saveourtool.save.spring.entity.BaseEntityWithDateAndDto
import javax.persistence.*

/**
 * @property contestSample contest sample
 * @property name name of field
 * @property type type of field
 * @property userId creator contestSampleField
 */
@Entity
class ContestSampleField(

    @ManyToOne
    @JoinColumn(name = "contest_sample_id")
    var contestSample: ContestSample,

    var name: String,

    @Enumerated(EnumType.STRING)
    var type: ContestSampleFieldType,

    var userId: Long,

) : BaseEntityWithDateAndDto<ContestSampleFieldDto>() {
    override fun toDto() = ContestSampleFieldDto(
        name = name,
        type = type,
    )
}
