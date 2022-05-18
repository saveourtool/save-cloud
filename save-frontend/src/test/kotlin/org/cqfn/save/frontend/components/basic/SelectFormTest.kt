package org.cqfn.save.frontend.components.basic

import org.cqfn.save.entities.Organization
import org.cqfn.save.frontend.externals.*
import org.cqfn.save.frontend.utils.apiUrl
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import react.create
import kotlin.test.*

class SelectFormTest {
    private val worker = setupWorker(
        rest.get("$apiUrl/organization/get/list") { req, res, ctx ->
            res(ctx.json(listOf(
                Organization.stub(1).apply { name = "Test Organization 1" },
                Organization.stub(2),
                Organization.stub(3),
            )))
        }
    )

    @BeforeTest
    fun setup() {
        worker.start()
    }

    @AfterTest
    fun tearDown() {
        worker.resetHandlers()
        // todo: stop worker in `afterall`. There doesn't seem to be immediate support for this in kotlin.test for JS.
        // Possible solution is to use https://kotest.io/docs/framework/lifecycle-hooks.html
//        worker.stop()
    }

    @Test
    fun select_form_should_show_organizations() {
        render(
            selectFormRequired { _, _, _ -> }.create {
                form = InputTypes.ORGANIZATION_NAME
                validInput = true
                classes = "col-md-6 pl-0 pl-2 pr-2"
                text = "Organization"
            }
        )

        screen.findByText<HTMLOptionElement>("Test Organization 1").then {
            val select = it.parentElement as HTMLSelectElement?
            assertNotNull(select, "`select` element should have been rendered")
            assertEquals(4, select.children.length, "Select should contain all organizations and an initial empty value")
        }
    }

    @Test
    fun component_should_contain_warning_if_no_organizations() {
        worker.use(
            rest.get("$apiUrl/organization/get/list") { _, res, ctx ->
                res(ctx.json(emptyList<Organization>()))
            }
        )

        render(
            selectFormRequired { _, _, _ -> }.create {
                form = InputTypes.ORGANIZATION_NAME
                validInput = true
                classes = "col-md-6 pl-0 pl-2 pr-2"
                text = "Organization"
            }
        )

        screen.findByText<HTMLDivElement>("You don't have access to any organizations").then {
            assertNotNull(it, "Component should display a warning if no organizations are available")
        }
    }
}