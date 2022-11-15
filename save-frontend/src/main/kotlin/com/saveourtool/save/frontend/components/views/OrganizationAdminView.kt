@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.frontend.components.basic.organizations.responseChangeOrganizationStatus
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.externals.fontawesome.faTrashAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.ErrorHandler
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.frontend.utils.noopResponseHandler
import csstype.ClassName
import react.ChildrenBuilder
import react.FC
import react.Fragment
import react.Props
import react.State
import react.create
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import react.table.columns
import kotlinx.coroutines.launch

/**
 * The list of all organizations, visible to super-users.
 */
internal class OrganizationAdminView : AbstractView<Props, OrganizationAdminState>(hasBg = false) {
    @Suppress("TYPE_ALIAS")
    private val organizationTable: FC<TableProps<Organization>> = tableComponent(
        columns = {
            columns {
                column(
                    id = "organization",
                    header = "Organization",
                    accessor = { name }
                ) { cellProps ->
                    val organizationName = cellProps.value

                    Fragment.create {
                        td {
                            Link {
                                to = "/organization/$organizationName/tools"
                                +organizationName
                            }
                        }
                    }
                }
                column(
                    id = "description",
                    header = "Description",
                    accessor = { description }
                ) { cellProps ->
                    Fragment.create {
                        td {
                            +(cellProps.value.orEmpty())
                        }
                    }
                }
                column(id = DELETE_BUTTON_COLUMN_ID, header = EMPTY_COLUMN_HEADER) { cellProps ->
                    Fragment.create {
                        td {
                            val organization = cellProps.value
                            val organizationName = organization.name

                            actionButton {
                                title = "WARNING: Delete Organization"
                                errorTitle = "You cannot delete a $organizationName"
                                message = "Are you sure you want to delete the organization $organizationName?"
                                clickMessage = "Change to ban mode"
                                buttonStyleBuilder = { childrenBuilder ->
                                    with(childrenBuilder) {
                                        fontAwesomeIcon(icon = faTrashAlt, classes = actionIconClasses.joinToString(" "))
                                    }
                                }
                                classes = actionButtonClasses.joinToString(" ")
                                modalButtons = { action, window, childrenBuilder ->
                                    with(childrenBuilder) {
                                        buttonBuilder(label = "Yes, delete $organizationName", style = "danger", classes = "mr-2") {
                                            action(1)
                                            window.closeWindow()
                                        }
                                        buttonBuilder("Cancel") {
                                            window.closeWindow()
                                        }
                                    }
                                }
                                onActionSuccess = { _, _ ->
                                    setState {
                                        organizations -= organization
                                    }
                                }
                                conditionClick = true
                                sendRequest = { isBanned, _ ->
                                    val newStatus = if (isBanned) OrganizationStatus.BANNED else OrganizationStatus.DELETED
                                    responseChangeOrganizationStatus(newStatus, organizationName)
                                }
                            }
                        }
                    }
                }
            }
        },
        initialPageSize = INITIAL_PAGE_SIZE,
        useServerPaging = false,
        usePageSelection = false,
        getAdditionalDependencies = { tableProps ->
            /*-
             * Necessary for the table to get re-rendered once an organization
             * gets deleted.
             *
             * The order and size of the array must remain constant.
             */
            arrayOf(tableProps)
        }
    )

    init {
        state.organizations = mutableListOf()
    }

    override fun componentDidMount() {
        super.componentDidMount()

        scope.launch {
            /*
             * Get the list of organizations and cache it forever.
             */
            val organizations = getOrganizations()
            setState {
                this.organizations = organizations
            }
        }
    }

    override fun ChildrenBuilder.render() {
        div {
            className = ClassName("row justify-content-center")

            div {
                className = ClassName("col-lg-10 mt-4 min-vh-100")

                organizationTable {
                    getData = { _, _ ->
                        /*
                         * Only reload (re-render) the table if the state gets
                         * updated.
                         */
                        state.organizations.toTypedArray()
                    }
                }
            }
        }
    }

    /**
     * @return the list of all organizations, excluding the deleted ones.
     */
    private suspend fun getOrganizations(): MutableList<Organization> {
        val response = get(
            url = "$apiUrl/organizations/all?onlyActive=${true}",
            headers = jsonHeaders,
            loadingHandler = ::classLoadingHandler,
        )

        return when {
            response.ok -> response.decodeFromJsonString()

            else -> mutableListOf()
        }
    }

    /**
     * Returns a lambda which, when invoked, deletes the specified organization
     * and updates the state of this view, passing an error message, if any, to
     * the externally supplied [ErrorHandler].
     *
     * @param organization the project to delete.
     * @return the lambda which deletes [organization].
     * @see ErrorHandler
     */
    private fun deleteOrganization(organization: Organization): suspend WithRequestStatusContext.(ErrorHandler) -> Unit = { errorHandler ->
        val response = post(
            "$apiUrl/organizations/${organization.name}/change-status?status=${OrganizationStatus.DELETED}",
            headers = jsonHeaders,
            body = undefined,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        )
        if (response.ok) {
            setState {
                /*
                 * Force the component to get re-rendered once an organization
                 * is deleted.
                 */
                organizations -= organization
            }
        } else {
            errorHandler(response.unpackMessageOrHttpStatus())
        }
    }

    private companion object {
        /**
         * The mandatory column id.
         * For each cell, this will be transformed into "cell_%d_delete_button"
         * and visible as the key in the "Components" tab of the developer tools.
         */
        private const val DELETE_BUTTON_COLUMN_ID = "delete_button"

        /**
         * Empty table header.
         */
        private const val EMPTY_COLUMN_HEADER = ""

        /**
         * The maximum number of items on a single page.
         */
        private const val INITIAL_PAGE_SIZE = 10
    }
}

/**
 * The state of the [OrganizationAdminView] component.
 *
 * @see OrganizationAdminView
 */
internal external interface OrganizationAdminState : State {
    /**
     * The cached list of all organizations.
     * Allows avoiding to run an `HTTP GET` each time an organization is deleted
     * (re-rendering gets triggered by updating the state instead).
     */
    var organizations: MutableList<Organization>
}
