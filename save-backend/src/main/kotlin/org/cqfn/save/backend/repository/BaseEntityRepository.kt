package org.cqfn.save.backend.repository

import org.cqfn.save.entities.BaseEntity
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import kotlin.reflect.KClass

/**
 * JPA repositories for the subclasses of the BaseEntity
 */
@NoRepositoryBean
interface BaseEntityRepository<T : BaseEntity> : JpaRepository<T, Long> {
    /**
     * @param entityClass
     * @param specification
     * @return list of class objects
     */
    fun getList(entityClass: KClass<T>, specification: Specification<T>): List<T>
}
