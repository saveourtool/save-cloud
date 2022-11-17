package com.saveourtool.save.demo.cpg.controller

import com.saveourtool.save.demo.cpg.entity.DemoEntity
import com.saveourtool.save.demo.cpg.repository.DemoEntityRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/demo-entity")
class DemoEntityController(
    private val demoEntityRepository: DemoEntityRepository,
) {
    /**
     * @param fromValue
     * @param toValue
     */
    @PostMapping("/upload")
    fun upload(
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
        fromDemoEntity.next.add(toDemoEntity)
        toDemoEntity.prev.add(fromDemoEntity)
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
    ): ResponseEntity<DemoEntity> {
        return demoEntityRepository.findByValue(value)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
    }
}