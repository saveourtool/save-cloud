package org.cqfn.save.preprocessor.controllers

import org.cqfn.save.preprocessor.dto.GitRepoDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import java.io.File

/**
 * A Spring controller for git project downloading
 */
@RestController
class DownloadProject {
    private val log = LoggerFactory.getLogger(DownloadProject::class.java)

    /**
     * @param gitRepoDto - Dto of repo information to clone
     */
    @PostMapping(value = ["/upload"])
    fun upload(@RequestBody gitRepoDto: GitRepoDto): ResponseEntity<String> {
        val user = if (gitRepoDto.username != null && gitRepoDto.password != null) {
            UsernamePasswordCredentialsProvider(gitRepoDto.username, gitRepoDto.password)
        } else {
            CredentialsProvider.getDefault()
        }
        try {
            Git.cloneRepository()
                .setURI(gitRepoDto.url)
                .setCredentialsProvider(user)
                .setDirectory(File(volumes))
                .call().use {
                    log.info("repository cloned")
                    // TODO post request to orchestrator
                    return ResponseEntity.ok("Cloned")
                }
        } catch (ex: TransportException) {
            log.warn("No credentials with private repo")
            return ResponseEntity.ok("No credentials")
        } catch (ex: Exception) {
            log.warn("Error to clone repo")
            return ResponseEntity.ok("Error to clone")
        }
    }

    companion object {
        private const val volumes = "~/var/lib/docker/volumes/repository"
    }
}
