@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.filters.OrganizationAdminFilters
import com.saveourtool.save.frontend.components.basic.OrganizationAdminFilterRowProps
import com.saveourtool.save.frontend.components.basic.organizationAdminRow
import com.saveourtool.save.frontend.components.basic.testExecutionFiltersRow
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.components.views.usersettings.responseDeleteOrganization
import com.saveourtool.save.frontend.components.views.usersettings.responseRecoverOrganization
import com.saveourtool.save.frontend.externals.fontawesome.faRedo
import com.saveourtool.save.frontend.externals.fontawesome.faTrashAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.classLoadingHandler

import csstype.BorderRadius
import csstype.ClassName
import kotlinx.browser.window
import react.ChildrenBuilder
import react.FC
import react.Fragment
import react.Props
import react.State
import react.create
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.td
import react.router.dom.Link
import react.table.columns

import kotlinx.coroutines.launch
import kotlinx.js.jso
import react.dom.html.ReactHTML


/**
 * The list of all organizations, visible to super-users.
 */
internal class OrganizationAdminView : AbstractView<OrganizationAdminFilterRowProps, OrganizationAdminState>(hasBg = false) {
    @Suppress("TYPE_ALIAS")
    private val organizationTable: FC<TableProps<Organization>> = tableComponent(
        columns = {
            columns {
                column(
                    id = "organization",
                    header = "Organization",
                    accessor = { name }
                ) { cellProps ->
                    val organization = cellProps.row.original
                    val organizationName = cellProps.value

                    Fragment.create {
                        td {
                            when (organization.status) {
                                OrganizationStatus.CREATED -> Link {
                                    to = "/organization/$organizationName/tools"
                                    +organizationName
                                }
                                OrganizationStatus.BANNED -> div {
                                    className = ClassName("text-danger")
                                    +cellProps.value
                                    span {
                                        className = ClassName("border ml-2 pr-1 pl-1 text-xs text-muted ")
                                        style = jso { borderRadius = "2em".unsafeCast<BorderRadius>() }
                                        +"banned"
                                    }
                                }
                                else -> {}
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

                            if (organization.status == OrganizationStatus.CREATED) {
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
                                            buttonBuilder(label = "Yes, ban $organizationName", style = "danger", classes = "mr-2") {
                                                action()
                                                window.closeWindow()
                                            }
                                            buttonBuilder("Cancel") {
                                                window.closeWindow()
                                            }
                                        }
                                    }
                                    onActionSuccess = { clickMode: Boolean ->
                                        setState {
                                            organizations -= organization
                                            if (clickMode)
                                                bannedOrganizations += organization.copy(status = OrganizationStatus.BANNED)
                                            else
                                                deletedOrganizations += organization.copy(status = OrganizationStatus.DELETED)
                                        }
                                    }
                                    conditionClick = true
                                    sendRequest = { isClickMode ->
                                        responseDeleteOrganization(isClickMode, organizationName)
                                    }
                                }
                            }
                            else if (organization.status == OrganizationStatus.DELETED) {
                                actionButton {
                                    title = "WARNING: recover Organization"
                                    errorTitle = "You cannot recover a $organizationName"
                                    message = "Are you sure you want to recover the organization $organizationName?"
                                    buttonStyleBuilder = { childrenBuilder ->
                                        with(childrenBuilder) {
                                            fontAwesomeIcon(icon = faRedo, classes = actionIconClasses.joinToString(" "))
                                        }
                                    }
                                    classes = actionButtonClasses.joinToString(" ")
                                    modalButtons = { action, window, childrenBuilder ->
                                        with(childrenBuilder) {
                                            buttonBuilder(label = "Yes, recover $organizationName", style = "warning", classes = "mr-2") {
                                                action()
                                                window.closeWindow()
                                            }
                                            buttonBuilder("Cancel") {
                                                window.closeWindow()
                                            }
                                        }
                                    }
                                    onActionSuccess = { _ ->
                                        setState {
                                            organizations += organization.copy(status = OrganizationStatus.CREATED)
                                            deletedOrganizations -= organization
                                        }
                                    }
                                    conditionClick = false
                                    sendRequest = { _ ->
                                        responseRecoverOrganization(organizationName)
                                    }
                                }
                            }
                            else if (organization.status == OrganizationStatus.BANNED) {
                                actionButton {
                                    title = "WARNING: recover Organization"
                                    errorTitle = "You cannot recover a $organizationName"
                                    message = "Are you sure you want to recover the organization $organizationName?"
                                    buttonStyleBuilder = { childrenBuilder ->
                                        with(childrenBuilder) {
                                            fontAwesomeIcon(icon = faRedo, classes = actionIconClasses.joinToString(" "))
                                        }
                                    }
                                    classes = actionButtonClasses.joinToString(" ")
                                    modalButtons = { action, window, childrenBuilder ->
                                        with(childrenBuilder) {
                                            buttonBuilder(label = "Yes, recover $organizationName", style = "warning", classes = "mr-2") {
                                                action()
                                                window.closeWindow()
                                            }
                                            buttonBuilder("Cancel") {
                                                window.closeWindow()
                                            }
                                        }
                                    }
                                    onActionSuccess = { _ ->
                                        setState {
                                            organizations += organization.copy(status = OrganizationStatus.CREATED)
                                            bannedOrganizations -= organization
                                        }
                                    }
                                    conditionClick = false
                                    sendRequest = { _ ->
                                        responseRecoverOrganization(organizationName)
                                    }
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
            arrayOf(tableProps, state.organizations, state.deletedOrganizations, state.bannedOrganizations)
        },
        commonHeader = { tableInstance ->
            ReactHTML.tr {
                ReactHTML.th {
                    colSpan = tableInstance.columns.size
                    organizationAdminRow {
                        filters = state.filters
                        onChangeFilters = { filterValue ->
                            setState { filters = filterValue.copy(organizationName = filterValue.organizationName, status = filterValue.status) }
                        }
                    }
                }
            }
        },
    )

    init {
        state.organizations = mutableListOf()
        state.deletedOrganizations = mutableListOf()
        state.bannedOrganizations = mutableListOf()
        state.filters = OrganizationAdminFilters.any
    }

    override fun componentDidMount() {
        super.componentDidMount()

        scope.launch {
            /*
             * Get the list of organizations and cache it forever.
             */
            val organizations = getOrganizations()
            setState {
                this.organizations = organizations.filter { it.status == OrganizationStatus.CREATED }.toMutableList()
                this.deletedOrganizations = organizations.filter { it.status == OrganizationStatus.DELETED }.toMutableList()
                this.bannedOrganizations = organizations.filter { it.status == OrganizationStatus.BANNED }.toMutableList()
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
                        (state.organizations + state.deletedOrganizations + state.bannedOrganizations).toTypedArray()
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
            url = "$apiUrl/organizations/all",
            headers = jsonHeaders,
            loadingHandler = ::classLoadingHandler,
        )

        return when {
            response.ok -> response.decodeFromJsonString()

            else -> mutableListOf()
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
     * The cached list of all active organizations.
     * Allows avoiding to run an `HTTP GET` each time an organization is banned
     */
    var organizations: MutableList<Organization>

    /**
     * The cached list of all banned organizations.
     * Allows avoiding to run an `HTTP GET` each time an organization is deleted
     */
    var deletedOrganizations: MutableList<Organization>

    /**
     * The cached list of all banned organizations.
     * Allows avoiding to run an `HTTP GET` each time an organization is deleted
     */
    var bannedOrganizations: MutableList<Organization>

    /**
     * All filters in one class property [name]
     */
    var filters: OrganizationAdminFilters
}
