package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.externals.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.v1
import dom.html.HTMLDivElement
import dom.html.HTMLOptionElement
import dom.html.HTMLSelectElement
import react.*
import kotlin.js.Promise
import kotlin.test.*

class SelectFormTest {
    @Suppress("TYPE_ALIAS")
    private val selectFormRequired: FC<SelectFormRequiredProps<Organization>> = selectFormRequired()
    private fun createWorker() = setupWorker(
        rest.get("$apiUrl/organizations/get/list") { _, res, _ ->
            res { response ->
                mockMswResponse(
                    response, listOf(
                        Organization.stub(1).apply { name = "Test Organization 1" },
                        Organization.stub(2),
                        Organization.stub(3),
                    )
                )
            }
        }
    )

    @Test
    fun selectFormShouldShowOrganizations(): Promise<Unit> {
        val worker = createWorker()
        return (worker.start() as Promise<*>).then {
            render(
                wrapper.create {
                    selectFormRequired {
                        getData = {
                            get(
                                "$apiUrl/organizations/get/list",
                                jsonHeaders,
                                loadingHandler = ::noopLoadingHandler,
                            )
                                .decodeFromJsonString()
                        }
                        dataToString = { it.name }
                        disabled = false
                        formType = InputTypes.ORGANIZATION_NAME
                        validInput = true
                        classes = "col-md-6 pl-0 pl-2 pr-2"
                        formName = "Organization"
                    }
                }
            )
        }
            .then {
                screen.findByTextAndCast<HTMLOptionElement>("Test Organization 1")
            }
            .then {
                val select = it.parentElement as HTMLSelectElement?
                assertNotNull(select, "`select` element should have been rendered")
                assertEquals(4, select.children.length, "Select should contain all organizations and an initial empty value")
            }
    }

    @Test
    fun componentShouldContainWarningIfNoOrganizations(): Promise<*> {
        val worker = createWorker()
        worker.use(
            rest.get("/api/$v1/organizations/get/list") { _, res, _ ->
                res { response ->
                    mockMswResponse(
                        response, emptyList<Organization>()
                    )
                }
            }
        )
        return (worker.start() as Promise<*>).then {
            render(
                wrapper.create {
                    selectFormRequired {
                        getData = {
                            get(
                                "$apiUrl/organizations/get/list",
                                jsonHeaders,
                                loadingHandler = ::noopLoadingHandler,
                            )
                                .decodeFromJsonString()
                        }
                        dataToString = { it.name }
                        disabled = false
                        formType = InputTypes.ORGANIZATION_NAME
                        validInput = true
                        classes = "col-md-6 pl-0 pl-2 pr-2"
                        formName = "Organization"
                        notFoundErrorMessage = "You don't have access to any organizations"
                    }
                }
            )
        }
            .then {
                screen.findByTextAndCast<HTMLDivElement>("You don't have access to any organizations")
            }
            .then {
                assertNotNull(it, "Component should display a warning if no organizations are available")
            }
    }
}
