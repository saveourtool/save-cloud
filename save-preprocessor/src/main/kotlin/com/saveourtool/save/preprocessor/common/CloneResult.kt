package com.saveourtool.save.preprocessor.common

import com.saveourtool.save.preprocessor.utils.GitCommitInfo
import java.nio.file.Path

/**
 * The result of running `git clone`.
 *
 * @property directory the local directory the repository has been cloned into.
 * @property gitCommitInfo the _Git_ commit metadata.
 */
data class CloneResult(
    val directory: Path,
    val gitCommitInfo: GitCommitInfo,
)
