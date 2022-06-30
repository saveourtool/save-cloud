package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.entities.TestSuitesSourceLog
import com.saveourtool.save.storage.AbstractFileBasedStorage

import org.springframework.stereotype.Service

import java.nio.file.Path

import kotlin.io.path.div
import kotlin.io.path.name

/**
 * Storage for test suites source snapshots
 */
@Service
class TestSuitesSourceStorage(
    configProperties: ConfigProperties,
    private val testSuitesSourceService: TestSuitesSourceService,
) : AbstractFileBasedStorage.WithProjectCoordinates<TestSuitesSourceLog>(Path.of(configProperties.fileStorage.location) / "tests-suites-source") {
    override fun buildInnerKeyAndReturnProjectPath(pathToContent: Path): Pair<TestSuitesSourceLog, Path> = Pair(
        TestSuitesSourceLog(
            source = testSuitesSourceService.getByName(pathToContent.parent.name),
            version = pathToContent.name
        ),
        pathToContent.parent.parent
    )

    override fun buildPathToContentFromProjectPath(projectPath: Path, innerKey: TestSuitesSourceLog): Path =
            projectPath.resolve(innerKey.source.name)
                .resolve(innerKey.version)
}
