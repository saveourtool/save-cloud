package com.saveourtool.save.preprocessor.common

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.preprocessor.service.GitPreprocessorService
import org.reactivestreams.Publisher

/**
 * Uses [GitPreprocessorService] to clone and process a repository.
 */
fun interface CloneAndProcessDirectoryAction<T : Publisher<*>> {
    /**
     * Clones and processes a repository identified by [gitDto].
     *
     * @param gitPreprocessorService [GitPreprocessorService] a service of git preprocessor.
     * @param gitDto the _Git_ URL along with optional credentials.
     * @param branchOrTagOrCommit either a branch name, or a tag name, or a
     *   commit hash.
     * @param repositoryProcessor the custom process action.
     * @return a custom [Publisher] returned by [repositoryProcessor].
     */
    fun cloneAndProcessDirectoryAsync(
        gitPreprocessorService: GitPreprocessorService,
        gitDto: GitDto,
        branchOrTagOrCommit: String,
        repositoryProcessor: GitRepositoryProcessor<T>,
    ): T
}
