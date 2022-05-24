package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.BaseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * JPA repositories for the subclasses of the BaseEntity
 */
@NoRepositoryBean
interface BaseEntityRepository<T : BaseEntity> : JpaRepository<T, Long>
