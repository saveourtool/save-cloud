package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.service.CommentService
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.entities.Comment
import com.saveourtool.save.entities.CommentDto
import com.saveourtool.save.utils.StringResponse
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.v1
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

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
        summary = "Save new comment.",
        description = "Save new comment.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully saved project problem")
    @PreAuthorize("permitAll()")
    @Suppress("TYPE_ALIAS")
    fun getAllBySection(
        @RequestBody section: String,
    ): Mono<List<CommentDto>> = blockingToMono {
        commentService.findAllBySection(section).map(Comment::toDto)
    }
}
