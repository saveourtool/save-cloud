package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.backend.utils.postJsonAndAssert
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Project
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import reactor.core.publisher.Mono
import java.util.Optional

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient(timeout = "60000")
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(StandardSuitesUpdateScheduler::class),
)
class DeleteEntitiesTest {
    @Autowired
    lateinit var webClient: WebTestClient
    @MockBean private lateinit var testExecutionRepository: TestExecutionRepository
    @MockBean private lateinit var agentStatusRepository: AgentStatusRepository
    @MockBean private lateinit var agentRepository: AgentRepository
    @MockBean private lateinit var executionRepository: ExecutionRepository
    @MockBean private lateinit var projectRepository: ProjectRepository
    @MockBean private lateinit var projectPermissionEvaluator: ProjectPermissionEvaluator

    @BeforeEach
    fun setUp() {
        // fixme: don't stub repositories and roll back transaction after the test (or make separate test for deletion logic in data layer)
        doNothing().whenever(testExecutionRepository).delete(any())
        doNothing().whenever(agentStatusRepository).delete(any())
        doNothing().whenever(agentRepository).delete(any())
        doNothing().whenever(executionRepository).delete(any())
        whenever(executionRepository.findById(any())).thenAnswer {
            Optional.of(Execution.stub(Project.stub(1)).apply { id = it.arguments[0] as Long })
        }
        whenever(projectRepository.findByNameAndOrganization(any(), any())).thenReturn(
            Project.stub(99).apply { id = 1 }
        )
        with(projectPermissionEvaluator) {
            whenever(any<Mono<Project?>>().filterByPermission(any(), any(), any())).thenCallRealMethod()
        }
        whenever(projectPermissionEvaluator.checkPermissions(any(), any(), any())).thenCallRealMethod()
        whenever(projectPermissionEvaluator.hasPermission(any(), any(), any())).thenAnswer {
            val authentication = it.arguments[0] as UsernamePasswordAuthenticationToken
            return@thenAnswer authentication.authorities.contains(SimpleGrantedAuthority("ROLE_ADMIN"))
        }
    }

    @Test
    @WithMockUser
    fun `should forbid deletion by ID for ordinary user`() {
        val ids = listOf(1L, 2L, 3L)
        deleteExecutionsAndAssert(ids) {
            expectStatus().isForbidden
        }
    }

    @Test
    @WithMockUser
    fun `should forbid deletion by ID for ordinary user on a private project`() {
        Mockito.reset(projectRepository, executionRepository)
        val privateProject = Project.stub(99).apply {
            id = 1
            public = false
        }
        whenever(projectRepository.findByNameAndOrganization(any(), any())).thenReturn(
            privateProject
        )
        whenever(executionRepository.findById(any())).thenAnswer {
            Optional.of(Execution.stub(privateProject).apply { id = it.arguments[0] as Long })
        }
        val ids = listOf(1L, 2L, 3L)

        deleteExecutionsAndAssert(ids) {
            expectStatus().isNotFound
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should delete by ID for project admin`() {
        val ids = listOf(1L, 2L, 3L)
        deleteExecutionsAndAssert(ids) {
            expectStatus().isOk
        }
    }

    @Test
    @WithMockUser
    fun `should forbid deletion of all executions for ordinary user`() {
        deleteAllExecutionsAndAssert("huaweiName", 1) {
            expectStatus().isForbidden
        }
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `should delete all executions for project admin`() {
        deleteAllExecutionsAndAssert("huaweiName", 1) {
            expectStatus().isOk
        }
    }

    private fun deleteExecutionsAndAssert(executionIds: List<Long>, assert: ResponseSpec.() -> Unit) {
        webClient.postJsonAndAssert(
            uri = "/api/execution/delete?executionIds=${executionIds.joinToString(",")}",
            assert = assert
        )
    }

    private fun deleteAllExecutionsAndAssert(name: String, organizationId: Long, assert: ResponseSpec.() -> Unit) {
        webClient.postJsonAndAssert(
            uri = "/api/execution/deleteAll?name=$name&organizationId=$organizationId",
            assert = assert
        )
    }
}
