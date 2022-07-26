package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.service.GitService
import com.saveourtool.save.backend.utils.switchToNotFoundIfEmpty
import com.saveourtool.save.entities.GitDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@RestController
@RequestMapping("/internal/git")
class GitController(
    private val gitService: GitService,
) {
    @GetMapping
    fun getById(@RequestParam id: Long): Mono<GitDto> = gitService.findById(id)
        .toMono()
        .switchToNotFoundIfEmpty {
            "Git entity not found by id $id"
        }
}