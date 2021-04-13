package org.cqfn.save.backend.repository

import org.cqfn.save.entities.BaseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * JPA repositories for the subclasses of the BaseEntity
 */
@Repository
interface BaseEntityRepository<T : BaseEntity> : JpaRepository<T, Long>
