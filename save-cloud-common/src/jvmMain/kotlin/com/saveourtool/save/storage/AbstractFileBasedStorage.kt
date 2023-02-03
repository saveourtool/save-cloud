package com.saveourtool.save.storage

import com.saveourtool.save.utils.collectToFile
import com.saveourtool.save.utils.countPartsTill
import com.saveourtool.save.utils.switchIfEmptyToResponseException
import com.saveourtool.save.utils.toDataBufferFlux
import org.springframework.http.HttpStatus

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.io.IOException

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

import kotlin.io.path.*

/**
 * File based implementation of Storage
 *
 * @param rootDir root directory for storage
 * @param pathPartsCount amount of parts in path for key used for validation and filtering unexpected paths,
 * if it's null -- this validation is not applicable
 * @param storageIgnore list of filenames that should not be treated as files contained in storage (equivalent of .gitignore),
 * by default .DS_Store is ignored.
 * @param K type of key
 */
abstract class AbstractFileBasedStorage<K>(
    private val rootDir: Path,
    private val pathPartsCount: Int? = null,
    private val storageIgnore: Set<String> = setOf(".DS_Store"),
) : Storage<K> {
    init {
        rootDir.createDirectoriesIfRequired()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override fun list(): Flux<K> = Files.walk(rootDir)
        .toFlux()
        .filter { pathToContent ->
            pathToContent.isRegularFile() &&
                    pathPartsCount?.let { pathToContent.countPartsTill(rootDir) == it } ?: true &&
                    pathToContent.name !in storageIgnore &&
                    isKey(rootDir, pathToContent)
        }
        .map { buildKey(rootDir, it) }

    override fun doesExist(key: K): Mono<Boolean> = Mono.fromCallable { buildPathToContent(key).exists() }

    override fun contentLength(key: K): Mono<Long> = Mono.fromCallable { buildPathToContent(key).fileSize() }

    override fun lastModified(key: K): Mono<Instant> = Mono.fromCallable { buildPathToContent(key).getLastModifiedTime().toInstant() }

    override fun delete(key: K): Mono<Boolean> {
        val contentPath = buildPathToContent(key)
        return Mono.fromCallable {
            contentPath.deleteIfExists()
        }.doOnNext {
            if (it) {
                contentPath.parent.deleteDirectoriesTill(rootDir)
            }
        }
    }

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> {
        val contentPath = buildPathToContent(key)
        return Mono.fromCallable {
            contentPath.parent.createDirectoriesIfRequired()
            contentPath.createFile()
        }.flatMap {
            content.collectToFile(contentPath)
        }.map { it.toLong() }
    }

    override fun move(source: K, target: K): Mono<Boolean> {
        val sourceContentPath = buildPathToContent(source)
        val targetContentPath = buildPathToContent(target)
        return Mono.fromCallable {
            targetContentPath.parent.createDirectoriesIfRequired()
            sourceContentPath.moveTo(targetContentPath)
        }
            .thenReturn(true)
            .onErrorReturn(IOException::class.java, false)
    }

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<Unit> = upload(key, content)
        .filter { it == contentLength }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            "Invalid contentLength provided"
        }
        .thenReturn(Unit)

    override fun download(key: K): Flux<ByteBuffer> = buildPathToContent(key).toDataBufferFlux()
        .map { it.asByteBuffer() }

    /**
     * @param rootDir
     * @param pathToContent
     * @return true if provided path is key for content of this storage otherwise - false
     */
    protected open fun isKey(rootDir: Path, pathToContent: Path): Boolean = true

    /**
     * @param rootDir
     * @param pathToContent
     * @return [K] object is built by [Path]
     */
    protected abstract fun buildKey(rootDir: Path, pathToContent: Path): K

    /**
     * @param rootDir
     * @param key
     * @return [Path] is built by [K] object
     */
    protected abstract fun buildPathToContent(rootDir: Path, key: K): Path

    private fun buildPathToContent(key: K): Path = buildPathToContent(rootDir, key)

    private fun Path.createDirectoriesIfRequired() {
        if (!exists()) {
            createDirectories()
        }
    }

    private fun Path.deleteDirectoriesTill(stopDirectory: Path) {
        if (this != stopDirectory && this.listDirectoryEntries().isEmpty()) {
            this.deleteExisting()
            this.parent.deleteDirectoriesTill(stopDirectory)
        }
    }
}
