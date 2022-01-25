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
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import java.io.File

private val log = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

/**
 * @param gitDto
 * @param tmpDir
 * @param branchOrCommit
 * @return jGit git entity
 */
fun pullOrCloneProjectWithSpecificBranch(gitDto: GitDto, tmpDir: File, branchOrCommit: String?): Git? {
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
 * @param branchOrCommit
 * @return jGit git entity
 */
fun pullProject(
    gitDto: GitDto,
    tmpDir: File,
    userCredentials: CredentialsProvider?,
    branchOrCommit: String?
): Git? {
    log.info("Starting pull project ${gitDto.url} into the $tmpDir")
    val git = Git.open(tmpDir)

    log.debug("Reset all changes in $tmpDir before pull command")
    git.reset()
        .setMode(ResetCommand.ResetType.HARD)
        .call()

    if (!switchBranch(git, gitDto.url, branchOrCommit)) {
        return null
    }

    val branchName = git.repository.branch

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
 * @return flag, whether the switching was successful
 */
@Suppress("FUNCTION_BOOLEAN_PREFIX")
fun switchBranch(git: Git, repoUrl: String, branchOrCommit: String?): Boolean {
    val branchName = if (branchOrCommit.isNullOrBlank()) getDefaultBranchName(repoUrl) else branchOrCommit
    log.info("Start switch branch from ${git.repository.branch} to the $branchName for $repoUrl")
    branchName ?: run {
        log.error("Branch name wasn't provided and couldn't get default branch for repo $repoUrl")
        return false
    }

    if (git.repository.branch == branchName.replace("${Constants.DEFAULT_REMOTE_NAME}/", "")) {
        log.warn("Requested branch $branchName for git checkout command equals to the current branch, won't provide any actions")
        return true
    }

    try {
        try {
            checkout(git, branchName, true)
        } catch (ex: RefAlreadyExistsException) {
            log.warn("Branch $branchName for $repoUrl already exists, won't create it one more time")
            checkout(git, branchName, false)
        }
    } catch (ex: RefNotFoundException) {
        log.warn("Provided branch/commit $branchName wasn't found, will use current branch: ${git.repository.branch}")
    }

    return true
}

private fun getDefaultBranchName(repoUrl: String): String? {
    val head: Ref? = Git.lsRemoteRepository().setRemote(repoUrl).callAsMap()["HEAD"]
    val defaultBranch = head?.let {
        if (head.isSymbolic) {
            // Branch name
            head.target.name
        } else {
            // SHA-1 hash
            head.objectId.name
        }
    }
        ?: run {
            log.error("Couldn't get default branch name for $repoUrl")
            return null
        }

    log.debug("Getting default branch name: $defaultBranch")

    return defaultBranch.replace("refs/heads/", "${Constants.DEFAULT_REMOTE_NAME}/")
}

private fun checkout(git: Git, branchOrCommit: String, setCreateBranchFlag: Boolean) {
    git.checkout()
        // We need to call this method anyway, in aim not to have `detached head` state,
        // however, it will throw an exception, if branch already exists
        .setCreateBranch(setCreateBranchFlag)
        .setName(branchOrCommit.replace("${Constants.DEFAULT_REMOTE_NAME}/", ""))
        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
        .setStartPoint(branchOrCommit)
        .call()
}
