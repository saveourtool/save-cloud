package org.cqfn.save.preprocessor.controllers

import org.cqfn.save.preprocessor.Response
import org.cqfn.save.repository.GitRepository

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
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
    fun upload(@RequestBody gitRepository: GitRepository): Response {
        val urlHash = gitRepository.url.hashCode()
        val tmpDir = File("$volumes/$urlHash")
        if (tmpDir.exists()) {
            tmpDir.deleteRecursively()
            log.info("For ${gitRepository.url} repository: dir $urlHash was deleted")
        }
        tmpDir.mkdirs()
        log.info("For ${gitRepository.url} repository: dir $urlHash was created")
        val userCredentials = if (gitRepository.username != null && gitRepository.password != null) {
            UsernamePasswordCredentialsProvider(gitRepository.username, gitRepository.password)
        } else {
            CredentialsProvider.getDefault()
        }
        try {
            Git.cloneRepository()
                .setURI(gitRepository.url)
                .setCredentialsProvider(userCredentials)
                .setDirectory(tmpDir)
                .call().use {
                    log.info("Repository cloned: ${gitRepository.url}")
                    // TODO post request to orchestrator
                    return Mono.just(ResponseEntity("Cloned", HttpStatus.ACCEPTED))
                }
        } catch (invalidRemoteException: InvalidRemoteException) {
            log.warn("Invalidate remote exception while cloning ${gitRepository.url} repository", invalidRemoteException)
            return Mono.just(ResponseEntity(invalidRemoteException.stackTraceToString(), HttpStatus.BAD_GATEWAY))
        } catch (transportException: TransportException) {
            log.warn("Transport operation failed while cloning ${gitRepository.url} repository", transportException)
            return Mono.just(ResponseEntity(transportException.stackTraceToString(), HttpStatus.NOT_ACCEPTABLE))
        } catch (gitAPIException: GitAPIException) {
            log.warn("Error with git API while cloning ${gitRepository.url} repository", gitAPIException)
            return Mono.just(ResponseEntity(gitAPIException.stackTraceToString(), HttpStatus.BAD_REQUEST))
        } catch (ex: Exception) {
            log.warn("Cloning ${gitRepository.url} repository failed", ex)
            return Mono.just(ResponseEntity(ex.stackTraceToString(), HttpStatus.INTERNAL_SERVER_ERROR))
        }
    }
}
