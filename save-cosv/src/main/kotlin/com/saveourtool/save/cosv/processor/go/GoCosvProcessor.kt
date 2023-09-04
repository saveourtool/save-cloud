package com.saveourtool.save.cosv.processor.go

import com.saveourtool.save.cosv.processor.AbstractCosvProcessor
import com.saveourtool.save.cosv.processor.CosvProcessor
import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.repository.CosvSchema

import org.springframework.stereotype.Component

import kotlinx.serialization.serializer

private typealias GoSchema = CosvSchema<GoUrl, GoImports, Unit, Unit>

/**
 * [CosvProcessor] for OSV from [Go Vulnerability Database](https://pkg.go.dev/vuln/)
 */
@Component
class GoCosvProcessor(
    cosvRepository: CosvRepository,
) : AbstractCosvProcessor<GoUrl, GoImports, Unit, Unit>(cosvRepository, serializer()) {
    override val id: String = "GO"
}
