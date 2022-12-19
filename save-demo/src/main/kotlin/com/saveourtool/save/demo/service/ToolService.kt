package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entity.GitRepo
import com.saveourtool.save.demo.entity.Snapshot
import com.saveourtool.save.demo.entity.Tool
import com.saveourtool.save.demo.repository.ToolRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ToolService(
    private val toolRepository: ToolRepository,
    private val gitRepoService: GitRepoService,
    private val snapshotService: SnapshotService,
) {
    private fun save(gitRepo: GitRepo, snapshot: Snapshot) = toolRepository.save(Tool(gitRepo, snapshot))

    /**
     * @param gitRepo
     * @param snapshot
     */
    @Transactional
    fun saveIfNotPresent(gitRepo: GitRepo, snapshot: Snapshot): Tool {
        val gitRepoFromDb = gitRepoService.saveIfNotPresent(gitRepo)
        val snapshotFromDb = snapshotService.saveIfNotPresent(snapshot)
        return toolRepository.findByGitRepoAndSnapshot(gitRepoFromDb, snapshotFromDb) ?: save(gitRepoFromDb, snapshotFromDb)
    }

    /**
     * @param gitRepo
     * @param version
     * @return
     */
    fun findByGitRepoAndVersion(gitRepo: GitRepo, version: String) = toolRepository.findByGitRepoAndSnapshotVersion(gitRepo, version)
}
