package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.frontend.components.errorStatusContext
import com.saveourtool.save.frontend.externals.*
import com.saveourtool.save.frontend.utils.apiUrl
import com.saveourtool.save.frontend.utils.wrapper
import com.saveourtool.save.v1
import kotlinx.browser.window
import kotlinx.js.jso
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import react.*
import kotlin.js.Promise
import kotlin.test.*

class SelectFormTest {
    private val worker = setupWorker(
        rest.get("$apiUrl/organization/get/list") { _, res, _ ->
            res { res ->
                res.status = 200
                res.headers.set("Content-Type", "application/json")
                res.body = Json.encodeToString(listOf(
                    Organization.stub(1).apply { name = "Test Organization 1" },
                    Organization.stub(2),
                    Organization.stub(3),
                ))
                res
            }
        }
    )

    @BeforeTest
    fun setup(): Promise<*> {
        return worker.start() as Promise<*>
    }

    @AfterTest
    fun tearDown() {
        worker.resetHandlers()
        // todo: stop worker in `afterall`. There doesn't seem to be immediate support for this in kotlin.test for JS.
        // Possible solution is to use https://kotest.io/docs/framework/lifecycle-hooks.html
        // worker.stop()
    }

    @Test
    fun selectFormShouldShowOrganizations(): Promise<Unit> {
        render(
            wrapper.create {
                (selectFormRequired { _, _, _ -> }) {
                    form = InputTypes.ORGANIZATION_NAME
                    validInput = true
                    classes = "col-md-6 pl-0 pl-2 pr-2"
                    text = "Organization"
                }
            }
        )

        return screen.findByTextAndCast<HTMLOptionElement>("Test Organization 1",).then {
            val select = it.parentElement as HTMLSelectElement?
            assertNotNull(select, "`select` element should have been rendered")
            assertEquals(4, select.children.length, "Select should contain all organizations and an initial empty value")
        }
    }

    @Test
    fun componentShouldContainWarningIfNoOrganizations(): Promise<*> {
        worker.use(
            rest.get("/api/$v1/organization/get/list") { _, res, ctx ->
                res(ctx.json(emptyList<Organization>()))
            }
        )

        render(
            wrapper.create {
                (selectFormRequired { _, _, _ -> }) {
                    form = InputTypes.ORGANIZATION_NAME
                    validInput = true
                    classes = "col-md-6 pl-0 pl-2 pr-2"
                    text = "Organization"
                }
            }
        )

        return screen.findByTextAndCast<HTMLDivElement>("You don't have access to any organizations").then {
            assertNotNull(it, "Component should display a warning if no organizations are available")
        }
    }
}
