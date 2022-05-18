package org.cqfn.save.backend.controller

import org.cqfn.save.backend.configs.NoopWebSecurityConfig
import org.cqfn.save.backend.controllers.OrganizationController
import org.cqfn.save.backend.repository.*
import org.cqfn.save.backend.security.OrganizationPermissionEvaluator
import org.cqfn.save.backend.service.LnkUserOrganizationService
import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.backend.service.UserDetailsService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.backend.utils.mutateMockedUser
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.*
import org.cqfn.save.v1

import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

import java.time.LocalDateTime
import java.util.*

@ActiveProfiles("test")
@WebFluxTest(controllers = [OrganizationController::class])
@Import(
    OrganizationService::class,
    OrganizationPermissionEvaluator::class,
    LnkUserOrganizationService::class,
    UserDetailsService::class,
    NoopWebSecurityConfig::class,
)
@AutoConfigureWebTestClient
@Suppress("UnsafeCallOnNullableType")
class OrganizationControllerTest {
    @MockBean
    private lateinit var organizationRepository: OrganizationRepository

    @MockBean
    private lateinit var lnkUserOrganizationRepository: LnkUserOrganizationRepository

    @Autowired
    private lateinit var webClient: WebTestClient

    @MockBean
    private lateinit var userRepository: UserRepository

    @Test
    @WithMockUser(value = "admin", roles = ["VIEWER"])
    fun `delete organization with owner permission`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }
        val organization = Organization(
            "OrgForTests",
            OrganizationStatus.CREATED,
            dateCreated = LocalDateTime.now(),
            ownerId = 1
        )
        val user = User("admin", "", Role.VIEWER.toString(), "")
        prepareForDeletionTesting(organization, user, Role.OWNER)
        webClient.delete()
            .uri("/api/$v1/organization/${organization.name}/delete")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @WithMockUser(value = "John Doe", roles = ["VIEWER"])
    fun `delete organization without owner permission`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 2)
        }
        val organization = Organization(
            "OrgForTests",
            OrganizationStatus.CREATED,
            dateCreated = LocalDateTime.now(),
            ownerId = 1
        )
        val user = User("John Doe", "", Role.VIEWER.toString(), "")
        prepareForDeletionTesting(organization, user, Role.VIEWER)
        webClient.delete()
            .uri("/api/$v1/organization/${organization.name}/delete")
            .exchange()
            .expectStatus()
            .isForbidden
    }

    private fun prepareForDeletionTesting(organization: Organization, user: User, userRole: Role) {
        given(lnkUserOrganizationRepository.findByUserIdAndOrganizationName(any(), any())).willReturn(
            LnkUserOrganization(organization, user, userRole)
        )
        given(organizationRepository.findByName(any())).willReturn(organization)
        whenever(organizationRepository.save(any())).thenReturn(organization)
        given(userRepository.findByName(any())).willReturn(Optional.of(user))
        given(userRepository.findByNameAndSource(any(), any())).willReturn(Optional.of(user))
    }
}
