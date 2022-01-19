/**
 * Git utilities that are used in preprocessor for download/clone/update git repo
 */

package org.cqfn.save.preprocessor.utils

import org.cqfn.save.entities.GitDto
import org.eclipse.jgit.api.Git
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
fun cloneFromGit(gitDto: GitDto, tmpDir: File): Git? {
    val userCredentials = if (gitDto.username != null && gitDto.password != null) {
        UsernamePasswordCredentialsProvider(gitDto.username, gitDto.password)
    } else {
        CredentialsProvider.getDefault()
    }
    return if (tmpDir.exists() && !tmpDir.list().isNullOrEmpty()) {
        log.info("Starting pull project ${gitDto.url} into the $tmpDir")
        val git = Git.open(tmpDir)
        val remoteName = Constants.DEFAULT_REMOTE_NAME
        val fullBranchName = git.repository.branch

        if (!fullBranchName.contains("$remoteName/")) {
            log.error("Provided branch $fullBranchName seems to be an detached commit, pull command won't be performed!")
            // FixMe should we delete this dir manually or here? or leave as is?
            return null
        }
        val branchName = fullBranchName.replace("$remoteName/", "")
        println("\n\n\nCurrent branch ${branchName}")
        git.pull()
            .setCredentialsProvider(userCredentials)
            .setRemote(remoteName)
            .setRemoteBranchName(branchName)
            .call()
        // Just nothing, since no new git instance was created
        null
    } else {
        log.info("Starting clone project ${gitDto.url} into the $tmpDir")
        Git.cloneRepository()
            .setURI(gitDto.url)
            .setCredentialsProvider(userCredentials)
            .setDirectory(tmpDir)
            .call()
    }
}

fun pullGitProject(gitDto: GitDto, tmpDir: File, userCredentials: CredentialsProvider): Boolean {
    if (tmpDir.exists() && !tmpDir.list().isNullOrEmpty()) {
        log.info("Starting pull project ${gitDto.url} into the $tmpDir")
        val git = Git.open(tmpDir)
        val remoteName = Constants.DEFAULT_REMOTE_NAME
        val fullBranchName = git.repository.branch

        if (!fullBranchName.contains("$remoteName/")) {
            log.error("Provided branch $fullBranchName seems to be an detached commit, pull command won't be performed!")
            return false
        }
        val branchName = fullBranchName.replace("$remoteName/", "")
        println("\n\n\nCurrent branch ${branchName}")
        git.pull()
            .setCredentialsProvider(userCredentials)
            .setRemote(remoteName)
            .setRemoteBranchName(branchName)
            .call()

        return true
    }
    return false
}