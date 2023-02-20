package com.saveourtool.save.demo.storage

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.entity.Dependency
import com.saveourtool.save.demo.repository.DependencyRepository
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.key.AbstractS3KeyEntityManager
import org.springframework.stereotype.Component

/**
 * [com.saveourtool.save.storage.key.S3KeyManager] for [DependencyStorage]
 */
@Component
class DependencyKeyManager(
    configProperties: ConfigProperties,
    repository: DependencyRepository,
) : AbstractS3KeyEntityManager<Dependency, DependencyRepository>(
    prefix = concatS3Key(configProperties.s3Storage.prefix, "deps"),
    repository = repository,
) {
    override fun findByContent(key: Dependency): Dependency? = repository.findByDemoAndVersionAndFileId(key.demo, key.version, key.fileId)
}
