package org.cqfn.save.backend.repository

import org.cqfn.save.backend.entities.TestEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TestEntityRepository : JpaRepository<TestEntity, Long>
