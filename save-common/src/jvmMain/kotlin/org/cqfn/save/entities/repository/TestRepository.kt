package org.cqfn.save.entities.repository

import org.cqfn.save.entities.TestFile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TestRepository: JpaRepository<TestFile, Int>;