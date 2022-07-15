package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.*
import com.saveourtool.save.frontend.externals.*
import com.saveourtool.save.frontend.utils.apiUrl
import com.saveourtool.save.frontend.utils.mockMswResponse
import com.saveourtool.save.frontend.utils.wrapper
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.LocalDateTime

import react.create
import react.react

import kotlin.js.Promise
import kotlin.test.*
import kotlinx.js.jso

class ProjectViewTest {
    private val testOrganization = Organization(
        "TestOrg",
        OrganizationStatus.CREATED,
        2,
        LocalDateTime(2022, 6, 1, 12, 25),
    )
    private val testProject = Project(
        "TestProject",
        null,
        "Project Description",
        ProjectStatus.CREATED,
        true,
        2,
        "email@test.org",
        organization = testOrganization,
    )
    private val testUserInfo = UserInfo(
        "TestUser",
        "basic",
        mapOf(testProject.name to Role.VIEWER),
        mapOf(testOrganization.name to Role.VIEWER),
        globalRole = Role.SUPER_ADMIN,
    )
    private val worker = setupWorker(
        rest.get("$apiUrl/projects/get/organization-name") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    testProject
                )
            }
        },
        rest.post("$apiUrl/projects/git") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    GitDto("")
                )
            }
        },
        rest.get("$apiUrl/projects/${testOrganization.name}/${testProject.name}/users/roles") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    testUserInfo.projects[testProject.name]
                )
            }
        },
        rest.get("$apiUrl/allStandardTestSuites") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    emptyList<TestSuiteDto>()
                )
            }
        },
        rest.get("$apiUrl/files/${testOrganization.name}/${testProject.name}/list") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    emptyList<FileInfo>()
                )
            }
        },
        rest.get("$apiUrl/latestExecution") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    0.toLong()
                )
            }
        },
        rest.get("$apiUrl/getTestRootPathByExecutionId") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    ""
                )
            }
        },
    )

    @BeforeTest
    fun setup(): Promise<*> = worker.start() as Promise<*>

    @AfterTest
    fun tearDown() {
        worker.resetHandlers()
        // todo: stop worker in `afterall`. There doesn't seem to be immediate support for this in kotlin.test for JS.
        // Possible solution is to use https://kotest.io/docs/framework/lifecycle-hooks.html
        // worker.stop()
    }

    @Test
    fun projectViewShouldRender(): Promise<Unit> {
        renderProjectView()
        return screen.findByText(
            "Project ${testProject.name}",
            waitForOptions = jso {
                timeout = 15000
            }
        )
            .then {
                assertNotNull(it, "Should show project name")
            }
    }

    @Test
    @Ignore
    fun shouldShowConfirmationWindowWhenDeletingProject(): Promise<Unit> {
        renderProjectView()
        return screen.findByText(
            "SETTINGS",
            waitForOptions = jso {
                timeout = 15000
            }
        )
            .then {
                userEvent.click(it)
            }
            .then { _: Unit ->
                screen.findByText("Delete project")
            }
            .then {
                userEvent.click(it)
            }
            .then { _: Unit ->
                screen.findByText("Ok")
            }
            .then {
                assertNotNull(it, "Should show confirmation window")
            }
    }

    private fun renderProjectView(userInfo: UserInfo = testUserInfo) = wrapper.create {
        ProjectView::class.react {
            owner = testOrganization.name
            name = testProject.name
            currentUserInfo = userInfo
        }
    }.let {
        render(it)
    }
}
