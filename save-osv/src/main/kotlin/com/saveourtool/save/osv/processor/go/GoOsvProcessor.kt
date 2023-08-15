package com.saveourtool.save.osv.processor.go

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.osv.processor.AbstractOsvProcessor
import com.saveourtool.save.osv.processor.OsvProcessor
import com.saveourtool.save.osv.storage.OsvStorage

import com.saveourtool.osv4k.OsvSchema
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.springframework.stereotype.Component

private typealias GoSchema = OsvSchema<GoUrl, GoImports, Unit, Unit>

/**
 * [OsvProcessor] for OSV from [Go Vulnerability Database](https://pkg.go.dev/vuln/)
 */
@Component
class GoOsvProcessor(
    osvStorage: OsvStorage,
) : AbstractOsvProcessor<GoUrl, GoImports, Unit, Unit>(osvStorage) {
    override val id: String = "GO"
    override val serializer: KSerializer<GoSchema> = serializer()

    override fun VulnerabilityDto.updateBySpecificFields(osv: GoSchema): VulnerabilityDto = copy(
        relatedLink = osv.databaseSpecific?.url,
    )
}
