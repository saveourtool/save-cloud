/**
 * File uploader of demo management card
 */

package com.saveourtool.save.frontend.components.basic.demo.management

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.frontend.components.basic.fileuploader.simpleFileUploader
import com.saveourtool.save.frontend.utils.apiUrl
import csstype.ClassName
import react.ChildrenBuilder
import react.StateSetter
import react.dom.html.AutoComplete
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input

/**
 * Display file uploader of demo management card
 *
 * @param demoDto currently configured [DemoDto]
 * @param setDemoDto callback to update state of [demoDto]
 * @param isDisabled flag that defines if input forms are disabled or not
 * @param githubProjectCoordinates GitHub's organization and project name wrapped into [ProjectCoordinates]
 * @param setGithubProjectCoordinates callback that updates [githubProjectCoordinates] state
 * @param setSelectedFileDtos update state of list of [FileDto] that should be connected with this demo
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "TOO_MANY_PARAMETERS",
    "TYPE_ALIAS",
    "LongParameterList"
)
internal fun ChildrenBuilder.renderFileUploading(
    demoDto: DemoDto,
    setDemoDto: StateSetter<DemoDto>,
    isDisabled: Boolean,
    githubProjectCoordinates: ProjectCoordinates,
    setGithubProjectCoordinates: StateSetter<ProjectCoordinates>,
    setSelectedFileDtos: StateSetter<List<FileDto>>,
) {
    div {
        className = ClassName("d-flex justify-content-between align-items-center")
        div {
            className = ClassName("col pl-0")
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "left"
            title = "Unfortunately, GitHub tool download is temporary disabled. Please, upload the tool manually."
            input {
                className = ClassName("form-control col mb-2")
                autoComplete = AutoComplete.off
                placeholder = "GitHub organization name"
                value = githubProjectCoordinates.organizationName
                disabled = true
                onChange = { event ->
                    setGithubProjectCoordinates {
                        it.copy(organizationName = event.target.value)
                    }
                }
            }
            input {
                className = ClassName("form-control col mb-2")
                autoComplete = AutoComplete.off
                placeholder = "GitHub project name"
                value = githubProjectCoordinates.projectName
                disabled = true
                onChange = { event ->
                    setGithubProjectCoordinates {
                        it.copy(projectName = event.target.value)
                    }
                }
            }
            input {
                className = ClassName("form-control col")
                autoComplete = AutoComplete.off
                placeholder = "Release tag"
                value = demoDto.vcsTagName
                disabled = true
                onChange = { event ->
                    setDemoDto { request ->
                        request.copy(vcsTagName = event.target.value)
                    }
                }
            }
        }
        div {
            +" or "
        }
        div {
            className = ClassName("col pr-0")
            div {
                simpleFileUploader {
                    buttonLabel = " Upload files"
                    getUrlForAvailableFilesFetch = {
                        with(demoDto) { "$apiUrl/files/$projectCoordinates/list" }
                    }
                    getUrlForDemoFilesFetch = {
                        with(demoDto) { "$apiUrl/demo/$projectCoordinates/list-file" }
                    }
                    getUrlForFileDeletion = {
                        with(demoDto) { "$apiUrl/demo/$projectCoordinates/delete?fileId=${it.id}" }
                    }
                    getUrlForFileUpload = {
                        with(demoDto) { "$apiUrl/files/$projectCoordinates/upload" }
                    }
                    updateFileDtos = { setSelectedFileDtos(it) }
                    this.isDisabled = isDisabled
                    uploadFilesButtonTooltip = "Executables / Configuration files / setup.sh"
                }
            }
        }
    }
}
