package com.saveourtool.save.demo.cpg.controller

import com.saveourtool.save.demo.cpg.service.CpgService
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

typealias CpgResult = Pair<List<Component>, Set<Node>>

/**
 * A simple controller
 *
 * @property cpgService
 */
@RestController
@RequestMapping("/cpg/api")
class CpgController(
    private val cpgService: CpgService,
) {
    /**
     * @param language
     * @param sourceCode
     * @return result of uploading, it contains ID to request the result further
     */
    @PostMapping("/upload-code")
    fun uploadCode(
        @RequestParam language: String,
        @RequestBody sourceCode: List<String>,
    ): ResponseEntity<CpgResult> {
        val result = cpgService.translate(language, sourceCode)
        return ResponseEntity.ok(result.components to result.additionalNodes)
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
