package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.ByteBufferFluxResponse
import com.saveourtool.save.backend.storage.AvatarKey
import com.saveourtool.save.backend.storage.AvatarStorage
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.switchIfEmptyToNotFound
import com.saveourtool.save.v1
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import kotlin.time.Duration.Companion.days

/**
 * Controller for working with avatars.
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "avatars"),
)
@RestController
@RequestMapping(path = ["/api/$v1/avatar"])
internal class AvatarController(
    private val avatarStorage: AvatarStorage,
) {
    @Operation(
        method = "GET",
        summary = "Download an avatar for user.",
        description = "Download an avatar for user.",
    )
    @Parameters(
        Parameter(name = "userName", `in` = ParameterIn.PATH, description = "user name", required = true),
        Parameter(name = "imageName", `in` = ParameterIn.PATH, description = "image name of avatar", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Returns content of the file.")
    @ApiResponse(responseCode = "404", description = "Execution with provided ID is not found.")
    @GetMapping("/users/{userName}")
    fun forUser(
        @PathVariable userName: String,
    ): Mono<ByteBufferFluxResponse> = AvatarKey(
        type = AvatarType.USER,
        objectName = userName,
    )
        .toMonoResponse()
        .switchIfEmptyToNotFound {
            "Not found avatar for user $userName with name $userName"
        }

    @Operation(
        method = "GET",
        summary = "Download an avatar for organization.",
        description = "Download an avatar for organization.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "organization name", required = true),
        Parameter(name = "imageName", `in` = ParameterIn.PATH, description = "image name of avatar", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Returns content of the file.")
    @ApiResponse(responseCode = "404", description = "Execution with provided ID is not found.")
    @GetMapping("/organizations/{organizationName}")
    fun forOrganization(
        @PathVariable organizationName: String,
    ): Mono<ByteBufferFluxResponse> = AvatarKey(
        type = AvatarType.ORGANIZATION,
        objectName = organizationName,
    )
        .toMonoResponse()
        .switchIfEmptyToNotFound {
            "Not found avatar for organization $organizationName with name $organizationName"
        }

    private fun AvatarKey.toMonoResponse(): Mono<ByteBufferFluxResponse> = this.toMono()
        .filterWhen(avatarStorage::doesExist)
        .map {
            ResponseEntity.ok()
                .body(avatarStorage.download(this))
        }

    companion object {
        private val longExpirationTime = 150.days
    }
}
