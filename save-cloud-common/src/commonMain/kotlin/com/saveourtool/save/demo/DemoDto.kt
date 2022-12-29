package com.saveourtool.save.demo

import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.Sdk
import kotlinx.serialization.Serializable

/**
 * @property projectCoordinates saveourtool project coordinates
 * @property vcsTagName release tag that defines the version to be fetched
 * @property runCommand command that is used to run demo on test file [fileName]
 * @property fileName name of an input file
 * @property sdk required sdk for tool run
 * @property configName name of config file or null if no config file is consumed
 * @property projectCoordinates GitHub project coordinates
 * @property githubProjectCoordinates
 */
@Serializable
data class DemoDto(
    val projectCoordinates: ProjectCoordinates,
    val vcsTagName: String,
    val runCommand: String,
    val fileName: String,
    val sdk: Sdk = Sdk.Default,
    val configName: String? = null,
    val githubProjectCoordinates: ProjectCoordinates? = null,
) {
    companion object {
        val empty = emptyForProject("", "")

        /**
         * @param organizationName saveourtool organization name
         * @param projectName saveourtool project name
         * @return [DemoDto] filled only with saveourtool project data
         */
        fun emptyForProject(organizationName: String, projectName: String) = DemoDto(
            ProjectCoordinates(organizationName, projectName),
            "",
            "",
            "",
            Sdk.Default,
            null,
            ProjectCoordinates("", ""),
        )
    }
}
