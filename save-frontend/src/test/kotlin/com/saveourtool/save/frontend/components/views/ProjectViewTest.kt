package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.entities.Project
import com.saveourtool.save.entities.ProjectStatus
import com.saveourtool.save.frontend.externals.*
import com.saveourtool.save.frontend.utils.apiUrl
import com.saveourtool.save.frontend.utils.mockMswResponse
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.LocalDateTime
import kotlinx.browser.window
import react.create
import react.react
import kotlin.js.Promise
import kotlin.test.*

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
        mapOf(testProject.name to Role.OWNER),
        mapOf(testOrganization.name to Role.OWNER),
        globalRole = Role.VIEWER,
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
        rest.delete("$apiUrl/projects/${testOrganization.name}/${testProject.name}/delete") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    Unit
                )
            }
        }
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

        return screen.findByText("Project ${testProject.name}").then {
            assertNotNull(it, "Should show project nam")
        }
    }

    @Test
    fun shouldDeleteProjectInSettingsMenu(): Promise<Unit> {
        val renderResult = renderProjectView()
        return screen.findByText("SETTINGS").then {
            userEvent.click(it)
        }.then { _: Unit ->
            screen.findByText("Delete project")
        }.then {
            userEvent.click(it)
        }.then { _: Unit ->
            screen.findByText("Ok")
        }.then {
            userEvent.click(it)
        }.then { _: Unit ->
            assertTrue(window.location.href == "${window.location.origin}/", window.location.href)
        }.then {
            console.log(window.location.href)
        }
    }

    private fun renderProjectView(userInfo: UserInfo = testUserInfo) = ProjectView::class.react
        .create {
            owner = testOrganization.name
            name = testProject.name
            currentUserInfo = userInfo
        }.let {
            render(it)
        }
}

