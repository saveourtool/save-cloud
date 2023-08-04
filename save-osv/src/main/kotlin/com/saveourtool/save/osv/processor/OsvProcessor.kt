package com.saveourtool.save.osv.processor

import com.saveourtool.save.entities.vulnerability.VulnerabilityDto

import com.saveourtool.osv4k.OsvSchema

// TODO: need to move to osv4k library
typealias AnyOsvSchema = OsvSchema<out Any, out Any, out Any, out Any>

/**
 * Processor of OSV entry which creates [VulnerabilityDto].
 *  to save required info in save database
 */
interface OsvProcessor<S : AnyOsvSchema> {
    /**
     * @param osv
     * @return [VulnerabilityDto]
     */
    fun <T : S> apply(osv: T): VulnerabilityDto
}
