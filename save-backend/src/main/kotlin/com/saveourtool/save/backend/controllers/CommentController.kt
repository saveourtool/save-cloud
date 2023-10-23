package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.security.CommentPermissionEvaluator
import com.saveourtool.save.backend.service.CommentService
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.entities.Comment
import com.saveourtool.save.entities.CommentDto
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.utils.StringResponse
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.switchIfEmptyToNotFound
import com.saveourtool.save.utils.switchIfEmptyToResponseException
import com.saveourtool.save.v1
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

/**
 * Controller for working with comments.
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "comments"),
)
@RestController
@RequestMapping(path = ["/api/$v1/comments"])
class CommentController(
    private val commentService: CommentService,
    private val commentPermissionEvaluator: CommentPermissionEvaluator,
) {
    @PostMapping("/save")
    @Operation(
        method = "POST",
        summary = "Save new comment.",
        description = "Save new comment.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully saved project problem")
    @PreAuthorize("permitAll()")
    fun save(
        @RequestBody comment: CommentDto,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono {
        if (comment.section.isEmpty()) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "section is empty")
        }
        commentService.saveComment(comment, authentication)
    }.map {
        ResponseEntity.ok("User comment was successfully saved")
    }

    @PostMapping("/get-all")
    @Operation(
        method = "POST",
        summary = "Get all comments in section.",
        description = "Get all comments in section.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully return all comments in section")
    @PreAuthorize("permitAll()")
    @Suppress("TYPE_ALIAS")
    fun getAllBySection(
        @RequestBody section: String,
    ): Mono<List<CommentDto>> = blockingToMono {
        commentService.findAllBySection(section).map(Comment::toDto)
    }

    @GetMapping("/get-all-count")
    @Operation(
        method = "Get",
        summary = "Get count comments in section.",
        description = "Get count comments in section.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully return count comments in section")
    @PreAuthorize("permitAll()")
    @Suppress("TYPE_ALIAS")
    fun getAllCountBySection(
        @RequestParam section: String,
    ): Mono<Int> = blockingToMono {
        commentService.countBySection(section)
    }

    @PostMapping("/delete")
    @Operation(
        method = "POST",
        summary = "Delete comment.",
        description = "Delete comment.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully deleted comment")
    @ApiResponse(responseCode = "403", description = "User permissions are not enough to delete requested comment")
    @ApiResponse(responseCode = "404", description = "Requested comment was not found")
    @PreAuthorize("permitAll()")
    fun deleteComment(
        @RequestBody comment: CommentDto,
        authentication: Authentication,
    ): Mono<StringResponse> = comment.toMono()
        .filter { commentPermissionEvaluator.hasPermission(authentication, it, Permission.DELETE) }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) { "Permissions required for comment deletion were not granted." }
        .flatMap { blockingToMono { commentService.deleteComment(it) } }
        .switchIfEmptyToNotFound { "Could not find requested comment." }
        .map { StringResponse.ok("Successfully deleted requested comment.") }
}
