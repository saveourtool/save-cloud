package org.cqfn.save.backend.repository

import org.cqfn.save.entities.BaseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * JPA repositories for the subclasses of the BaseEntity
 */
@NoRepositoryBean
interface BaseEntityRepository<T : BaseEntity> : JpaRepository<T, Long>
