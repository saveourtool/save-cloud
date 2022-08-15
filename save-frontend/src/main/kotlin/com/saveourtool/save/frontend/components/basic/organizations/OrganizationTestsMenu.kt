@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "TOP_LEVEL_ORDER"
)

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.components.basic.showTestSuiteSourceCreationModal
import com.saveourtool.save.frontend.components.tables.TableProps
import com.saveourtool.save.frontend.components.tables.tableComponent
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.loadingHandler
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.testsuite.TestSuitesSourceDtoList
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKeyList
import csstype.ClassName
import kotlinext.js.asJsObject
import org.w3c.dom.url.URLSearchParams
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.td
import react.table.columns



/**
 * TESTS tab in OrganizationView
 */
val organizationTestsMenu = organizationTestsMenu()

/**
 * OrganizationTestsMenu component props
 */
external interface OrganizationTestsMenuProps : Props {
    /**
     * Current organization name
     */
    var organizationName: String

    /**
     * [Role] of user that is observing this component
     */
    var selfRole: Role

    var onErrorMessage: (Response) -> Unit
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun organizationTestsMenu() = FC<OrganizationTestsMenuProps> { props ->
    val (isTestSuiteSourceCreationModalOpen, setIsTestSuitesSourceCreationModalOpen) = useState(false)

    val (testSuitesSources, setTestSuitesSources) = useState(emptyList<TestSuitesSourceDto>())
    val fetchTestSuitesSources = useRequest(dependencies = arrayOf(props.organizationName)) {
        val response = get(
            url = "$apiUrl/test-suites-sources/${props.organizationName}/list",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            response.unsafeMap {
                it.decodeFromJsonString<TestSuitesSourceDtoList>()
            }.let {
                setTestSuitesSources(it)
            }
        } else {
            setTestSuitesSources(emptyList())
        }
    }
    fetchTestSuitesSources()

    val (selectedTestSuitesSource, setSelectedTestSuitesSource) = useState<TestSuitesSourceDto?>(null)
    val (testSuitesSourceSnapshotKeys, setTestSuitesSourceSnapshotKeys) = useState(emptyList<TestSuitesSourceSnapshotKey>())
    val fetchTestSuitesSourcesSnapshotKeys = useRequest(dependencies = arrayOf(selectedTestSuitesSource)) {
        selectedTestSuitesSource?.let { testSuitesSource ->
            val response = get(
                url = "$apiUrl/test-suites-sources/${testSuitesSource.organizationName}/${encodeURIComponent(testSuitesSource.name)}/list-snapshot",
                headers = Headers().also {
                    it.set("Accept", "application/json")
                },
                loadingHandler = ::loadingHandler,
            )
            if (response.ok) {
                response.unsafeMap {
                    it.decodeFromJsonString<TestSuitesSourceSnapshotKeyList>()
                }.let {
                    setTestSuitesSourceSnapshotKeys(it)
                }
            } else {
                setTestSuitesSourceSnapshotKeys(emptyList())
            }
        }
    }
    val testSuitesSourcesTable = prepareTestSuitesSourcesTable {
        if (selectedTestSuitesSource == it) {
            setSelectedTestSuitesSource(null)
        } else {
            setSelectedTestSuitesSource(it)
            fetchTestSuitesSourcesSnapshotKeys()
        }
    }
    showTestSuiteSourceCreationModal(
        isTestSuiteSourceCreationModalOpen,
        props.organizationName,
        {
            setIsTestSuitesSourceCreationModalOpen(false)
            fetchTestSuitesSources()
        },
        {
            setIsTestSuitesSourceCreationModalOpen(false)
            props.onErrorMessage(it)
        },
    ) {
        setIsTestSuitesSourceCreationModalOpen(false)
    }
    div {
        className = ClassName("d-flex justify-content-center mb-3")
        button {
            type = ButtonType.button
            className = ClassName("btn btn-sm btn-primary")
            disabled = !props.selfRole.hasWritePermission()
            onClick = {
                setIsTestSuitesSourceCreationModalOpen(true)
            }
            +"+ Create test suites source"
        }
    }
    div {
        className = ClassName("mb-2")
        testSuitesSourcesTable {
            getData = { _, _ ->
                testSuitesSources.toTypedArray()
            }
            content = testSuitesSources
        }
    }

    selectedTestSuitesSource?.let {
        div {
            className = ClassName("mb-2")
            testSuitesSourceSnapshotKeysTable {
                getData = { _, _ ->
                    testSuitesSourceSnapshotKeys.toTypedArray()
                }
                content = testSuitesSourceSnapshotKeys
            }
        }
    }
}

/**
 * Extensions for [TableProps] which adds content field (where content of table is taken from external variable)
 */
external interface TablePropsWithContent<D : Any> : TableProps<D> {
    /**
     * Signal to update table
     */
    var content: List<D>

    var isTestSuiteSourceCreated: Boolean
}

@Suppress("MAGIC_NUMBER", "TYPE_ALIAS", "TOO_LONG_FUNCTION")
private fun prepareTestSuitesSourcesTable(
    selectHandler: (TestSuitesSourceDto) -> Unit,
): FC<TablePropsWithContent<TestSuitesSourceDto>> = tableComponent(
    columns = columns {
        column(id = "organizationName", header = "Organization", { this }) { cellProps ->
            Fragment.create {
                td {
                    onClick = {
                        selectHandler(cellProps.value)
                    }
                    +cellProps.value.organizationName
                }
            }
        }
        column(id = "name", header = "Name", { this }) { cellProps ->
            Fragment.create {
                td {
                    onClick = {
                        selectHandler(cellProps.value)
                    }
                    +cellProps.value.name
                }
            }
        }
        column(id = "description", header = "Description", { this }) { cellProps ->
            Fragment.create {
                td {
                    onClick = {
                        selectHandler(cellProps.value)
                    }
                    +(cellProps.value.description ?: "Description is not provided")
                }
            }
        }
        column(id = "location", header = "Git location", { this }) { cellProps ->
            Fragment.create {
                td {
                    onClick = {
                        selectHandler(cellProps.value)
                    }
                    a {
                        href = "${cellProps.value.gitDto.url}/tree/${cellProps.value.branch}/${cellProps.value.testRootPath}"
                        +"source"
                    }
                }
            }
        }
    },
    initialPageSize = 10,
    useServerPaging = false,
    usePageSelection = false,
    getAdditionalDependencies = {
        arrayOf(it.content, it.isTestSuiteSourceCreated)
    },
)

@Suppress("MAGIC_NUMBER", "TYPE_ALIAS")
private val testSuitesSourceSnapshotKeysTable: FC<TablePropsWithContent<TestSuitesSourceSnapshotKey>> = tableComponent(
    columns = columns {
        column(id = "version", header = "Version", { version }) { cellProps ->
            Fragment.create {
                td {
                    +cellProps.value
                }
            }
        }
        column(id = "creationTime", header = "Creation Time", { convertAndGetCreationTime() }) { cellProps ->
            Fragment.create {
                td {
                    +cellProps.value.toString()
                }
            }
        }
    },
    initialPageSize = 10,
    useServerPaging = false,
    usePageSelection = false,
    getAdditionalDependencies = {
        arrayOf(it.content)
    },
)
