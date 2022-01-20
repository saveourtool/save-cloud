/**
 * Git utilities that are used in preprocessor for download/clone/update git repo
 */

package org.cqfn.save.preprocessor.utils

import org.cqfn.save.entities.GitDto
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import org.eclipse.jgit.api.errors.RefNotAdvertisedException
import org.eclipse.jgit.api.errors.RefNotFoundException
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
fun pullOrCloneProjectWithSpecificBranch(gitDto: GitDto, tmpDir: File, branchOrCommit: String?): Git? {
    println("\n\n\npullOrCloneFromGit")
    val userCredentials = if (gitDto.username != null && gitDto.password != null) {
        UsernamePasswordCredentialsProvider(gitDto.username, gitDto.password)
    } else {
        CredentialsProvider.getDefault()
    }

    if (tmpDir.exists() && !tmpDir.list().isNullOrEmpty()) {
        pullProject(gitDto, tmpDir, userCredentials, branchOrCommit)?.let {
            return it
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
        .call().also { git ->
            switchBranch(git, gitDto.url, branchOrCommit)
    }
}

/**
 * @param gitDto
 * @param tmpDir
 * @param userCredentials
 * @return jGit git entity
 */
fun pullProject(gitDto: GitDto, tmpDir: File, userCredentials: CredentialsProvider?, branchOrCommit: String?): Git? {
    log.info("Starting pull project ${gitDto.url} into the $tmpDir")
    val git = Git.open(tmpDir)

    switchBranch(git, gitDto.url, branchOrCommit)

    log.debug("Reset all changes in $tmpDir before pull command")
    git.reset()
        .setMode(ResetCommand.ResetType.HARD)
        .call()

    val branchName = git.repository.branch
    println("\n\n\nCurrent branch $branchName")

    try {
        git.pull()
            .setCredentialsProvider(userCredentials)
            .setRemote(Constants.DEFAULT_REMOTE_NAME)
            .setRemoteBranchName(branchName)
            .setFastForward(MergeCommand.FastForwardMode.FF)
            .call()
    } catch (ex: RefNotAdvertisedException) {
        log.error("Provided branch $branchName seems to be an detached commit, pull command won't be performed!")
        return null
    } catch (ex: GitAPIException) {
        log.error("Error during pull project: ", ex)
        return null
    }
    log.info("Successfully pull branch $branchName for project ${gitDto.url}")
    return git
}

/**
 * @param git
 * @param repoUrl
 * @param branchOrCommit
 */
fun switchBranch(git: Git, repoUrl: String, branchOrCommit: String?) {
    println("\n\n\ngit.repository.branch ${git.repository.branch}")
    if (branchOrCommit == null || branchOrCommit.isBlank()) {
        return
    }
    if (git.repository.branch == branchOrCommit.replace("${Constants.DEFAULT_REMOTE_NAME}/", "")) {
        log.warn("Requested branch $branchOrCommit for git checkout command equals to the current branch, won't provide any actions")
        return
    }
    log.info("For $repoUrl switching to the $branchOrCommit")
    try {
//        git.checkout()
//            .setCreateBranch(true)
//            .setName(branchOrCommit.replace("${Constants.DEFAULT_REMOTE_NAME}/", ""))
//            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
//            .setStartPoint(branchOrCommit)
//            .call()
        checkout(git, branchOrCommit)
    } catch (ex: RefNotFoundException) {
        log.warn("Provided branch/commit $branchOrCommit wasn't found, will use default branch")
    }
}

private fun checkout(git: Git, branchOrCommit: String) {
    try {
        git.checkout()
            .setCreateBranch(true)
            .setName(branchOrCommit.replace("${Constants.DEFAULT_REMOTE_NAME}/", ""))
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            .setStartPoint(branchOrCommit)
            .call()
    } catch (ex: RefAlreadyExistsException) {
        git.checkout()
            .setCreateBranch(false)
            .setName(branchOrCommit.replace("${Constants.DEFAULT_REMOTE_NAME}/", ""))
            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            .setStartPoint(branchOrCommit)
            .call()
    }
}