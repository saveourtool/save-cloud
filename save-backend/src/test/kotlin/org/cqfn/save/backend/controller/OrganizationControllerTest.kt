package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.*
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.backend.utils.mutateMockedUser
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.*
import org.cqfn.save.v1

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDateTime

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@Suppress("UnsafeCallOnNullableType")
class OrganizationControllerTest {
    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var lnkUserOrganizationRepository: LnkUserOrganizationRepository

    @Autowired
    lateinit var webClient: WebTestClient

    @Test
    @WithUserDetails(value = "admin")
    fun `delete organization with owner permission`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 2)
        }
        val dateCreated = LocalDateTime.of(2022, 5, 16, 18, 16)
        val organization = Organization(
            "OrgForTests",
            OrganizationStatus.CREATED,
            dateCreated = dateCreated,
            ownerId = 1
        )
        organizationRepository.save(organization)
        val user = userRepository.findByName("admin").get()
        lnkUserOrganizationRepository.save(LnkUserOrganization(organization, user, Role.OWNER))

        webClient.delete()
            .uri("/api/$v1/organization/${organization.name}/delete")
            .exchange()
            .expectStatus()
            .isOk

        val organizationFromDb = organizationRepository.findByName(organization.name)
        Assertions.assertTrue(
            organizationFromDb?.status == OrganizationStatus.DELETED
        )
    }

    @Test
    @WithUserDetails(value = "John Doe")
    fun `delete organization without owner permission`() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 2)
        }
        val dateCreated = LocalDateTime.of(2022, 5, 16, 18, 16)
        val organization = Organization(
            "OrgForTests",
            OrganizationStatus.CREATED,
            dateCreated = dateCreated,
            ownerId = 1
        )
        organizationRepository.save(organization)
        val user = userRepository.findByName("John Doe").get()
        lnkUserOrganizationRepository.save(LnkUserOrganization(organization, user, Role.VIEWER))

        webClient.delete()
            .uri("/api/$v1/organization/${organization.name}/delete")
            .exchange()
            .expectStatus()
            .isForbidden

        val organizationFromDb = organizationRepository.findByName(organization.name)
        Assertions.assertTrue(
            organizationFromDb?.status == OrganizationStatus.CREATED
        )
    }
}
