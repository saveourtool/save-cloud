package com.saveourtool.save.demo.cpg.repository

import com.saveourtool.save.demo.cpg.entity.DemoEntity
import org.springframework.data.neo4j.repository.Neo4jRepository

/**
 * A repository for [DemoEntity]
 */
interface DemoEntityRepository : Neo4jRepository<DemoEntity, Long> {
    /**
     * @param value
     * @return found [DemoEntity]
     */
    fun findByValue(value: String): DemoEntity?
}
