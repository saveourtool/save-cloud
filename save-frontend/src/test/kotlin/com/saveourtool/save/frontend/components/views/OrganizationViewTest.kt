package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.frontend.externals.*
import com.saveourtool.save.frontend.utils.apiUrl
import com.saveourtool.save.frontend.utils.mockMswResponse
import com.saveourtool.save.frontend.utils.wrapper
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.LocalDateTime
import react.create
import react.react
import kotlin.js.Promise
import kotlin.test.*

class OrganizationViewTest {
    private val testOrganization = Organization(
        "TestOrg",
        OrganizationStatus.CREATED,
        2,
        LocalDateTime(2022, 6, 1, 12, 25),
    )
    private val testUserInfo = UserInfo(
        "TestUser",
        "basic",
        emptyMap(),
        mapOf(testOrganization.name to Role.OWNER),
        globalRole = Role.VIEWER,
    )
    private val worker = setupWorker(
        rest.get("$apiUrl/organization/${testOrganization.name}") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response,
                    testOrganization
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
    fun shouldShowConfirmationWindowWhenDeletingOrganization(): Promise<Unit> {
        renderOrganizationView()
        screen.getByText("SETTINGS").let {
            userEvent.click(it)
        }
        return screen.findByText("Delete organization").then {
            userEvent.click(it)
        }.then { _: Unit ->
            screen.findByText("Ok")
        }
            .then {
                assertNotNull(it, "Should show confirmation window")
            }
    }

    private fun renderOrganizationView(userInfo: UserInfo = testUserInfo) = wrapper.create {
        OrganizationView::class.react {
            organizationName = testOrganization.name
            currentUserInfo = userInfo
        }
    }
        .let {
            render(it)
        }
}
