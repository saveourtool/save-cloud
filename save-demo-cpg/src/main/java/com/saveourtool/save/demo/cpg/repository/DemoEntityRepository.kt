package com.saveourtool.save.demo.cpg.repository

import com.saveourtool.save.demo.cpg.entity.DemoEntity
import org.springframework.data.neo4j.repository.Neo4jRepository

interface DemoEntityRepository : Neo4jRepository<DemoEntity, Long> {
    fun findByValue(value: String): DemoEntity?
}