package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.diktat.DiktatDemoTool
import com.saveourtool.save.demo.service.GithubDownloadToolService
import com.saveourtool.save.demo.storage.ToolStorage
import com.saveourtool.save.demo.storage.toToolKey
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class GithubDownloadToolServiceTest(
//    private val githubDownloadToolService: GithubDownloadToolService,
//    private val toolStorage: ToolStorage,
)
{
    @Test
    fun `test correct download from gitHub, upload to and download from storage`() {
        val elem = DiktatDemoTool.DIKTAT.toToolKey("diktat-1.2.3.jar")
//        githubDownloadToolService.downloadFromGithubAndUploadToStorage(elem)
//        val content = toolStorage.download(elem)
    }
}