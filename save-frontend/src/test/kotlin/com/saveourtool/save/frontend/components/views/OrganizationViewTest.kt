package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.*
import com.saveourtool.save.frontend.externals.*
import com.saveourtool.save.frontend.utils.apiUrl
import com.saveourtool.save.frontend.utils.mockMswResponse
import com.saveourtool.save.frontend.utils.wrapper
import com.saveourtool.save.info.UserInfo
import kotlinx.datetime.LocalDateTime

import react.create
import react.react
import react.router.MemoryRouter

import kotlin.js.Promise
import kotlin.test.*
import js.core.jso

class OrganizationViewTest {
    private val testOrganization = OrganizationDto.empty
        .copy(
            name = "TestOrg",
            dateCreated = LocalDateTime(2022, 6, 1, 12, 25),
        )
    private val testUserInfo = UserInfo(
        "TestUser",
        source = "basic",
        projects = emptyMap(),
        organizations = mapOf(testOrganization.name to Role.ADMIN),
        globalRole = Role.SUPER_ADMIN,
    )

    @Suppress("TOO_LONG_FUNCTION")
    private fun createWorker() = setupWorker(
        rest.get("$apiUrl/organizations/${testOrganization.name}") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    testOrganization,
                )
            }
        },
        rest.post("$apiUrl/projects/by-filters") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    arrayListOf<ProjectDto>()
                )
            }
        },
        rest.get("$apiUrl/organizations/${testOrganization.name}/users/roles") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    testUserInfo.organizations[testOrganization.name],
                )
            }
        },
        rest.get("$apiUrl/organizations/${testOrganization.name}/users") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    listOf(testUserInfo),
                )
            }
        },
    )

    @Test
    fun shouldShowConfirmationWindowWhenDeletingOrganization(): Promise<*> {
        val worker = createWorker()
        return (worker.start() as Promise<*>).then {
            renderOrganizationView()
        }
            .then {
                screen.findByText(
                    "SETTINGS",
                    waitForOptions = jso {
                        timeout = 15000
                    },
                )
            }
            .then {
                userEvent.click(it)
            }
            .then { _: Unit ->
                screen.findByText("Delete ${testOrganization.name}")
            }
            .then {
                userEvent.click(it)
            }
            .then { _: Unit ->
                screen.findByText("Yes, delete ${testOrganization.name}")
            }
            .then {
                assertNotNull(it, "Should show confirmation window")
            }
            .then {
                worker.stop()
            }
    }

    private fun renderOrganizationView(userInfo: UserInfo = testUserInfo) = wrapper.create {
        MemoryRouter {
            OrganizationView::class.react {
                organizationName = testOrganization.name
                currentUserInfo = userInfo
            }
        }
    }
        .let {
            render(it)
        }
}
