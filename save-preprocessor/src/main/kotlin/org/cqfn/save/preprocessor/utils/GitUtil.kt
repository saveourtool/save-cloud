/**
 * Git utilities that are used in preprocessor for download/clone/update git repo
 */

package org.cqfn.save.preprocessor.utils

import org.cqfn.save.entities.GitDto
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.RefNotAdvertisedException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import java.io.File

private val log = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

/**
 * @param gitDto
 * @param tmpDir
 * @return jGit git entity
 */
fun pullOrCloneFromGit(gitDto: GitDto, tmpDir: File): Git? {
    println("\n\n\npullOrCloneFromGit")
    val userCredentials = if (gitDto.username != null && gitDto.password != null) {
        UsernamePasswordCredentialsProvider(gitDto.username, gitDto.password)
    } else {
        CredentialsProvider.getDefault()
    }
    // FixMe tmpDir.list() contain .git dir?
    if (tmpDir.exists() && !tmpDir.list().isNullOrEmpty()) {
        val ok = pullGitProject(gitDto, tmpDir, userCredentials)
        if (ok) {
            // Just nothing, since no new git instance was created
            return null
        }
        // Pull was unsuccessful, clean directory before cloning
        deleteDirectory(tmpDir)
        generateDirectory(tmpDir)
    }
    log.info("Starting clone project ${gitDto.url} into the $tmpDir")
    return Git.cloneRepository()
        .setURI(gitDto.url)
        .setCredentialsProvider(userCredentials)
        .setDirectory(tmpDir)
        .call()
}

fun pullGitProject(gitDto: GitDto, tmpDir: File, userCredentials: CredentialsProvider?): Boolean {
        log.info("Starting pull project ${gitDto.url} into the $tmpDir")
        val git = Git.open(tmpDir)
        val remoteName = Constants.DEFAULT_REMOTE_NAME
        val fullBranchName = git.repository.branch
        println("\n\n\nCurrent branch ${fullBranchName}")
        val branchName = fullBranchName.replace("$remoteName/", "")

        log.info("Reset all changes in $tmpDir before pull command")
        git.reset()
            .setMode(ResetCommand.ResetType.HARD)
            .call()

        try {
            git.pull()
                .setCredentialsProvider(userCredentials)
                .setRemote(remoteName)
                .setRemoteBranchName(branchName)
                .setFastForward(MergeCommand.FastForwardMode.FF)
                .call()
        } catch (ex: RefNotAdvertisedException) {
            log.error("Provided branch $fullBranchName seems to be an detached commit, pull command won't be performed!")
            return false
        }
        return true
}