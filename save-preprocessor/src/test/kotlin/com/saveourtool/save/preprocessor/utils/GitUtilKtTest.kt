package com.saveourtool.save.preprocessor.utils

import com.saveourtool.save.entities.GitDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.div

internal class GitUtilKtTest {
    @Test
    fun test(@TempDir tmpDir: Path) {
        val gitDto = GitDto("https://github.com/saveourtool/save-cli.git")

        gitDto.cloneTagToDirectory("v0.3.5", tmpDir / "tag")
        gitDto.cloneBranchToDirectory("infra/build-logic-includebuild", tmpDir / "branch")
        gitDto.cloneCommitToDirectory("8a8f164", tmpDir / "commit")
    }
}
