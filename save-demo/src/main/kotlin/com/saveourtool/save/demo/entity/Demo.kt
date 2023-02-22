package com.saveourtool.save.demo.entity

import com.saveourtool.save.demo.DemoConfiguration
import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.RunConfiguration
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.toSdk
import com.saveourtool.save.spring.entity.BaseEntityWithDto
import javax.persistence.Column
import javax.persistence.Entity

/**
 * Entity that encapsulates all the information required for tool download and run
 *
 * @property organizationName name of organization from saveourtool
 * @property projectName name of project from saveourtool
 * @property sdk sdk required for demo run
 * @property runCommand command that runs the tool on test file with name [fileName]
 * @property fileName name that the tested input file should have
 * @property configName name of tool config file (or null if no config is needed)
 * @property outputFileName name of output file (or null if [outputFileName] is [fileName])
 * @property githubOrganizationName name of organization from GitHub
 * @property githubProjectName name of project from GitHub
 */
@Entity
@Suppress("LongParameterList")
class Demo(
    var organizationName: String,
    var projectName: String,
    var sdk: String,
    var runCommand: String,
    var fileName: String,
    var configName: String?,
    var outputFileName: String?,
    @Column(name = "github_organization")
    var githubOrganizationName: String?,
    @Column(name = "github_project")
    var githubProjectName: String?,
) : BaseEntityWithDto<DemoDto>() {
    private fun githubProjectCoordinates() = githubOrganizationName?.let { organization ->
        githubProjectName?.let { project ->
            ProjectCoordinates(
                organization,
                project,
            )
        }
    }

    /**
     * @return saveourtool [ProjectCoordinates]
     */
    fun projectCoordinates() = ProjectCoordinates(
        organizationName,
        projectName,
    )

    override fun toDto(): DemoDto = DemoDto(
        projectCoordinates(),
        "",
        runCommand,
        fileName,
        sdk.toSdk(),
        configName,
        outputFileName,
        githubProjectCoordinates(),
    )

    /**
     * @return [RunConfiguration] for agent filled with data from this [Demo]
     */
    fun toRunConfiguration() = RunConfiguration(
        fileName,
        configName,
        runCommand,
        outputFileName,
    )

    /**
     * @param version version that is requested for demo
     * @return [DemoConfiguration] for agent filled with data from this [Demo]
     */
    fun toDemoConfiguration(version: String) = DemoConfiguration(
        organizationName,
        projectName,
        version,
    )
}

/**
 * @return [Demo] entity filled with [DemoDto] data
 */
fun DemoDto.toDemo() = Demo(
    projectCoordinates.organizationName,
    projectCoordinates.projectName,
    sdk.toString(),
    runCommand,
    fileName,
    configName,
    outputFileName,
    githubProjectCoordinates?.organizationName,
    githubProjectCoordinates?.projectName,
)
