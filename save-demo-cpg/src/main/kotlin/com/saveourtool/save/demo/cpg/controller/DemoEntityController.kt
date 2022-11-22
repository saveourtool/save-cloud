package com.saveourtool.save.demo.cpg.controller

import com.saveourtool.save.demo.cpg.entity.DemoEntity
import com.saveourtool.save.demo.cpg.repository.DemoEntityRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * A controller for [DemoEntity]
 */
@RestController
@RequestMapping("/demo-entity")
class DemoEntityController(
    private val demoEntityRepository: DemoEntityRepository,
) {
    /**
     * @param fromValue
     * @param toValue
     * @return result of operation
     */
    @PostMapping("/map")
    fun map(
        @RequestParam("from") fromValue: String,
        @RequestParam("to") toValue: String,
    ): ResponseEntity<Unit> {
        if (fromValue == toValue) {
            return ResponseEntity.badRequest().build()
        }
        val fromDemoEntity = demoEntityRepository.findByValue(fromValue)
            ?: DemoEntity(fromValue)
        val toDemoEntity = demoEntityRepository.findByValue(toValue)
            ?: DemoEntity(toValue)
        fromDemoEntity.next = fromDemoEntity.next + toDemoEntity
        toDemoEntity.prev = toDemoEntity.prev + fromDemoEntity
        demoEntityRepository.saveAll(setOf(fromDemoEntity, toDemoEntity))
        return ResponseEntity.ok(Unit)
    }

    /**
     * @param value
     * @return result found by provided [value]
     */
    @GetMapping("/get")
    fun get(
        @RequestParam value: String,
    ): ResponseEntity<DemoEntity> = demoEntityRepository.findByValue(value)?.let {
        ResponseEntity.ok(it)
    } ?: ResponseEntity.notFound().build()
}
