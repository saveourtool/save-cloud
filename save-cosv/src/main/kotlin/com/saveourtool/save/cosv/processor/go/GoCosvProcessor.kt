package com.saveourtool.save.cosv.processor.go

import com.saveourtool.save.cosv.processor.AbstractCosvProcessor
import com.saveourtool.save.cosv.processor.CosvProcessor
import com.saveourtool.save.cosv.repository.CosvSchema
import com.saveourtool.save.cosv.storage.CosvStorage
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto

import org.springframework.stereotype.Component

import kotlinx.serialization.serializer

private typealias GoSchema = CosvSchema<GoUrl, GoImports, Unit, Unit>

/**
 * [CosvProcessor] for OSV from [Go Vulnerability Database](https://pkg.go.dev/vuln/)
 */
@Component
class GoCosvProcessor(
    cosvStorage: CosvStorage,
) : AbstractCosvProcessor<GoUrl, GoImports, Unit, Unit>(cosvStorage, serializer()) {
    override val id: String = "GO"

    override fun VulnerabilityDto.updateBySpecificFields(osv: GoSchema): VulnerabilityDto = copy(
        relatedLink = osv.databaseSpecific?.url,
    )
}
