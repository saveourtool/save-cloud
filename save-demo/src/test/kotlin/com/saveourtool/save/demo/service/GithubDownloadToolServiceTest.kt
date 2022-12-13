package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.storage.ToolStorage
import io.ktor.util.*
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import kotlin.io.path.Path

@Component
class GithubDownloadToolServiceTest(
    private val githubDownloadToolService: GithubDownloadToolService,
    private val toolStorage: ToolStorage,
)
{

    @Test
    fun `connect to server`() {
        githubDownloadToolService.downloadFromGithubAndUploadToStorage()
    }
}