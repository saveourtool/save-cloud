package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.storage.ToolStorage
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Component

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