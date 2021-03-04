package org.cqfn.save.preprocessor.controllers

import org.cqfn.save.repository.GitRepository
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

import java.io.File

/**
 * A Spring controller for git project downloading
 */
@RestController
class DownloadProject {
    private val log = LoggerFactory.getLogger(DownloadProject::class.java)

    @Value("\${save.repository}")
    private lateinit var volumes: String

    /**
     * @param gitRepository - Dto of repo information to clone
     * @return response entity with text
     */
    @Suppress("TooGenericExceptionCaught")
    @PostMapping(value = ["/upload"])
    fun upload(@RequestBody gitRepository: GitRepository): Mono<ResponseEntity<String>> {
        val urlHash = gitRepository.url.hashCode()
        val tmpDir = File("$volumes/$urlHash")
        tmpDir.deleteRecursively()
        log.info("For ${gitRepository.url} repository: dir $urlHash was created")
        val user = if (gitRepository.username != null && gitRepository.password != null) {
            UsernamePasswordCredentialsProvider(gitRepository.username, gitRepository.password)
        } else {
            CredentialsProvider.getDefault()
        }
        try {
            Git.cloneRepository()
                .setURI(gitRepository.url)
                .setCredentialsProvider(user)
                .setDirectory(tmpDir)
                .call().use {
                    log.info("Repository cloned: ${gitRepository.url}")
                    // TODO post request to orchestrator
                    return Mono.just(ResponseEntity("Cloned", HttpStatus.ACCEPTED))
                }
        } catch (ex: Exception) {
            log.warn("Cloning ${gitRepository.url} repository failed",ex)
            return Mono.just(ResponseEntity(ex.stackTraceToString(), HttpStatus.BAD_REQUEST))
        }
    }
}
