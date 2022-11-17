package com.saveourtool.save.demo.cpg.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * A simple controller
 */
@RestController
@RequestMapping("/cpg/api")
class CpgController {
    /**
     * @param language
     * @param sourceCode
     * @return result of uploading, it contains ID to request the result further
     */
    @PostMapping("/upload-code")
    fun uploadCode(
        @RequestParam language: String,
        @RequestBody sourceCode: List<String>,
    ): ResponseEntity<String> = ResponseEntity.ok("N/A")

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
