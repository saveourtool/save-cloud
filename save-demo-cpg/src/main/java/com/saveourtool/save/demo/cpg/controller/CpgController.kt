package com.saveourtool.save.demo.cpg.controller

import com.saveourtool.save.demo.cpg.service.CpgService
import org.springframework.data.neo4j.core.Neo4jOperations
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

typealias CpgResult = Pair<Int, Int>

/**
 * A simple controller
 *
 * @property cpgService
 * @property neo4jOperations
 */
@RestController
@RequestMapping("/cpg/api")
class CpgController(
    private val cpgService: CpgService,
    private val neo4jOperations: Neo4jOperations,
) {
    /**
     * @param language
     * @param sourceCode
     * @return result of uploading, it contains numbers of components and additional nodes
     */
    @Transactional
    @PostMapping("/upload-code")
    fun uploadCode(
        @RequestParam language: String,
        @RequestBody sourceCode: List<String>,
    ): ResponseEntity<CpgResult> {
        val result = cpgService.translate(language, sourceCode)
        neo4jOperations.saveAll(result.components)
        neo4jOperations.saveAll(result.additionalNodes)
        return ResponseEntity.ok(result.components.size to result.additionalNodes.size)
    }

    /**
     * @param uploadId
     * @return result of translation
     */
    @GetMapping("/get-result")
    fun getResult(
        @RequestParam uploadId: String,
    ): ResponseEntity<String> = ResponseEntity.ok("""
            {
                "node": "value"
            }
        """.trimIndent())
}
