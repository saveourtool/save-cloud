package com.saveourtool.save.preprocessor.utils

import com.saveourtool.save.entities.GitDto
import org.eclipse.jgit.util.FileUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.util.FileSystemUtils
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting

internal class GitUtilKtTest {
    @Test
    fun test() {
        val tmpDir = createTempDirectory()
        val gitDto = GitDto("https://github.com/saveourtool/save-cli.git")

        gitDto.cloneTagToDirectory("v0.3.4", tmpDir.resolve("tag"))
        gitDto.cloneBranchToDirectory("infra/build-logic-includebuild", tmpDir.resolve("branch"))
        gitDto.cloneCommitToDirectory("8a8f164", tmpDir.resolve("commit"))

        FileSystemUtils.deleteRecursively(tmpDir)
    }
}
