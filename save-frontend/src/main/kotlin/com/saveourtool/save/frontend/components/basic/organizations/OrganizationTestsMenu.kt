@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "TOP_LEVEL_ORDER"
)

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.components.basic.organizations.testsuitespermissions.PermissionManagerMode
import com.saveourtool.save.frontend.components.basic.organizations.testsuitespermissions.manageTestSuitePermissionsComponent
import com.saveourtool.save.frontend.components.basic.testsuitessources.fetch.testSuitesSourceFetcher
import com.saveourtool.save.frontend.components.basic.testsuitessources.showTestSuiteSourceUpsertModal
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.loadingHandler
import com.saveourtool.save.testsuite.*

import csstype.ClassName
import react.*
import react.dom.html.ReactHTML.div

import kotlinx.browser.window

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
}

private suspend fun WithRequestStatusContext.getTestSuitesSourcesWithId(
    organizationName: String,
) = get(
    url = "$apiUrl/test-suites-sources/$organizationName/list",
    headers = jsonHeaders,
    loadingHandler = ::loadingHandler,
)

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun organizationTestsMenu() = FC<OrganizationTestsMenuProps> { props ->
    useTooltip()
    val testSuitesSourceUpsertWindowOpenness = useWindowOpenness()
    val (isSourceCreated, setIsSourceCreated) = useState(false)
    val (testSuitesSources, setTestSuitesSources) = useState(emptyList<TestSuitesSourceDto>())
    useRequest(dependencies = arrayOf(props.organizationName, isSourceCreated)) {
        val response = getTestSuitesSourcesWithId(props.organizationName)
        if (response.ok) {
            setTestSuitesSources(response.decodeFromJsonString<TestSuitesSourceDtoList>())
        } else {
            setTestSuitesSources(emptyList())
        }
    }
    val (testSuiteSourceToFetch, setTestSuiteSourceToFetch) = useState<TestSuitesSourceDto>()
    val testSuitesSourceFetcherWindowOpenness = useWindowOpenness()
    testSuitesSourceFetcher(
        testSuitesSourceFetcherWindowOpenness,
        testSuiteSourceToFetch ?: TestSuitesSourceDto.empty
    )

    val (selectedTestSuitesSource, setSelectedTestSuitesSource) = useState<TestSuitesSourceDto>()
    val (testSuitesSourceSnapshotKeys, setTestSuitesSourceSnapshotKeys) = useState(emptyList<TestSuitesSourceSnapshotKey>())
    val fetchTestSuitesSourcesSnapshotKeys = useDeferredRequest {
        selectedTestSuitesSource?.let { testSuitesSource ->
            val response = get(
                url = "$apiUrl/test-suites-sources/${testSuitesSource.organizationName}/${encodeURIComponent(testSuitesSource.name)}/list-snapshot",
                headers = jsonHeaders,
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

    val (testSuitesSourceSnapshotKeyToDelete, setTestSuitesSourceSnapshotKeyToDelete) = useState<TestSuitesSourceSnapshotKey>()
    val deleteTestSuitesSourcesSnapshotKey = useDeferredRequest {
        testSuitesSourceSnapshotKeyToDelete?.let { key ->
            delete(
                url = "$apiUrl/test-suites-sources/${key.organizationName}/${encodeURIComponent(key.testSuitesSourceName)}/delete-test-suites-and-snapshot?version=${key.version}",
                headers = jsonHeaders,
                loadingHandler = ::loadingHandler,
            )
            setTestSuitesSourceSnapshotKeyToDelete(null)
        }
    }
    val selectHandler: (TestSuitesSourceDto) -> Unit = {
        if (selectedTestSuitesSource == it) {
            setSelectedTestSuitesSource(null)
        } else {
            setSelectedTestSuitesSource(it)
            fetchTestSuitesSourcesSnapshotKeys()
        }
    }
    val fetchHandler: (TestSuitesSourceDto) -> Unit = {
        setTestSuiteSourceToFetch(it)
        testSuitesSourceFetcherWindowOpenness.openWindow()
    }
    val (testSuiteSourceToUpsert, setTestSuiteSourceToUpsert) = useState<TestSuitesSourceDto>()
    val editHandler: (TestSuitesSourceDto) -> Unit = {
        setTestSuiteSourceToUpsert(it)
        testSuitesSourceUpsertWindowOpenness.openWindow()
    }
    val deleteHandler: (TestSuitesSourceSnapshotKey) -> Unit = {
        if (window.confirm("Are you sure you want to delete snapshot ${it.version} of ${it.testSuitesSourceName}?")) {
            setTestSuitesSourceSnapshotKeyToDelete(it)
            deleteTestSuitesSourcesSnapshotKey()
            setTestSuitesSourceSnapshotKeys(testSuitesSourceSnapshotKeys.filterNot(it::equals))
        }
    }
    val refreshTestSuitesSourcesSnapshotKey = useDeferredRequest {
        val response = getTestSuitesSourcesWithId(props.organizationName)
        if (response.ok) {
            setTestSuitesSources(response.decodeFromJsonString<TestSuitesSourceDtoList>())
        } else {
            setTestSuitesSources(emptyList())
        }
    }
    val refreshHandler: () -> Unit = {
        refreshTestSuitesSourcesSnapshotKey()
        fetchTestSuitesSourcesSnapshotKeys()
    }
    val (managePermissionsMode, setManagePermissionsMode) = useState<PermissionManagerMode?>(null)
    manageTestSuitePermissionsComponent {
        organizationName = props.organizationName
        isModalOpen = managePermissionsMode != null
        closeModal = { setManagePermissionsMode(null) }
        mode = managePermissionsMode
    }
    showTestSuiteSourceUpsertModal(
        windowOpenness = testSuitesSourceUpsertWindowOpenness,
        testSuitesSourceWithId = testSuiteSourceToUpsert,
        organizationName = props.organizationName,
    ) {
        setIsSourceCreated { !it }
    }

    div {
        className = ClassName("d-flex justify-content-center mb-3")
        buttonBuilder("+ Create test suites source", "primary", !props.selfRole.hasWritePermission(), classes = "btn-sm mr-2") {
            testSuitesSourceUpsertWindowOpenness.openWindow()
        }
        buttonBuilder("Manage permissions", "info", !props.selfRole.hasWritePermission(), classes = "btn-sm mr-2 ml-2") {
            setManagePermissionsMode(PermissionManagerMode.TRANSFER)
        }
        buttonBuilder("Publish test suites", "info", !props.selfRole.hasWritePermission(), classes = "btn-sm ml-2") {
            setManagePermissionsMode(PermissionManagerMode.PUBLISH)
        }
    }

    div {
        className = ClassName("mb-2 d-flex justify-content-center")
        when (selectedTestSuitesSource) {
            null -> showTestSuitesSources(testSuitesSources, selectHandler, fetchHandler, editHandler, refreshHandler)
            else -> showTestSuitesSourceSnapshotKeys(
                selectedTestSuitesSource,
                testSuitesSourceSnapshotKeys,
                selectHandler,
                editHandler,
                fetchHandler,
                deleteHandler,
                refreshHandler,
            )
        }
    }
}
