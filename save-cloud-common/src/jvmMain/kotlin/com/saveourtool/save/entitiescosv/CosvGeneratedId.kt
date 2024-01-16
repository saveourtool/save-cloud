package com.saveourtool.save.entitiescosv

import com.saveourtool.save.spring.entity.BaseEntityWithDate
import javax.persistence.Entity

/**
 * Entity for generated id of vulnerability
 */
@Entity
class CosvGeneratedId : BaseEntityWithDate() {
    /**
     * @return Vulnerability identifier for saved entity
     */
    fun getIdentifier(): String = "COSV-${requiredCreateDate().year}-${requiredId()}"
}
