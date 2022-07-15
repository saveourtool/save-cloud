/**
 * Git utilities that are used in preprocessor for download/clone/update git repo
 */

package com.saveourtool.save.preprocessor.utils

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.utils.debug
import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidConfigurationException
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import org.eclipse.jgit.api.errors.RefNotAdvertisedException
import org.eclipse.jgit.api.errors.RefNotFoundException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.Path

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
        val result = pullProject(gitDto, tmpDir, userCredentials, branchOrCommit)
        if (result.isFailure) {
            log.error(result.toString())
        } else {
            return result.getOrNull()
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
        .also { git ->
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
): Result<Git> {
    log.info("Starting pull project ${gitDto.url} into the $tmpDir")
    val git = Git.open(tmpDir)

    log.debug("Reset all changes in $tmpDir before pull command")
    git.reset()
        .setMode(ResetCommand.ResetType.HARD)
        .call()

    if (!switchBranch(git, gitDto.url, branchOrCommit)) {
        git.close()
        return Result.failure(InvalidConfigurationException("Error: cannot switch branch"))
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
        git.close()
        return Result.failure(
            IllegalArgumentException("Provided branch $branchName seems to be an detached commit, pull command won't be performed! $ex", ex)
        )
    } catch (ex: GitAPIException) {
        git.close()
        return Result.failure(Exception("Error during pull project: ", ex))
    }
    log.info("Successfully pull branch $branchName for project ${gitDto.url}")
    return Result.success(git)
}

/**
 * @param git
 * @param repoUrl
 * @param branchOrCommit
 * @return flag, whether the switching was successful
 */
@Suppress("FUNCTION_BOOLEAN_PREFIX")
fun switchBranch(git: Git, repoUrl: String, branchOrCommit: String?): Boolean {
    val branchName = if (branchOrCommit.isNullOrBlank()) {
        log.info("Branch name wasn't provided, will checkout to the default branch")
        getDefaultBranchName(repoUrl)
    } else {
        branchOrCommit
    }
    log.info("Start switch branch from ${git.repository.branch} to the $branchName for $repoUrl")
    branchName ?: run {
        log.error("Couldn't get default branch for repo $repoUrl")
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

/**
 * @param httpUrl
 * @param password
 * @param username
 * @return default branch name
 * @throws IllegalStateException when failed to detect default branch name
 */
fun detectDefaultBranchName(httpUrl: String, username: String?, password: String?): String {
    return Git.lsRemoteRepository()
        .setCredentialsProvider(credentialsProvider(username, password))
        // ls without clone
        // FIXME: need to extract to a common place
        .setRemote(httpUrl)
        .withRethrow {
            it.callAsMap()[Constants.HEAD]
        }
        ?.takeIf { it.isSymbolic }
        ?.target
        ?.name
        ?.also { defaultBranch ->
            log.debug { "Getting default branch name $defaultBranch for httpUrl $httpUrl" }
        }
        ?.replace(Constants.R_HEADS, "${Constants.DEFAULT_REMOTE_NAME}/")
        ?: throw IllegalStateException("Couldn't detect default branch name for $httpUrl")
}

/**
 * @return default branch name
 * @throws IllegalStateException when failed to detect default branch name
 */
fun GitDto.detectDefaultBranchName() = detectDefaultBranchName(url, username, password)

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


/**
 * @return latest commit
 */
fun GitDto.detectLatestSha1(branch: String): String = Git.lsRemoteRepository()
    .setCredentialsProvider(credentialsProvider())
    .setRemote(url)
    .withRethrow {
        it.callAsMap()["${Constants.R_HEADS}$branch"]
    }
    ?.objectId
    ?.name
    ?: throw IllegalStateException("Couldn't detect hash of ${Constants.HEAD} for $url/$branch")

/**
 * @param gitDto
 * @param branch
 * @param sha1
 * @param pathToDirectory
 * @throws IllegalStateException
 */
fun cloneToDirectory(gitDto: GitDto, branch: String, sha1: String, pathToDirectory: Path) {
    Git.cloneRepository()
        .setCredentialsProvider(gitDto.credentialsProvider())
        .setURI(gitDto.url)
        .setDirectory(pathToDirectory.toFile())
        .setRemote(Constants.DEFAULT_REMOTE_NAME)
        .setNoCheckout(true)
        .setNoTags()
        .setBranch(branch)
        .setCloneAllBranches(false)
        .withRethrow { it.call() }
        .use {
            it.checkout()
                .setName(sha1)
                .call()
        }
}

private fun GitDto.credentialsProvider(): CredentialsProvider = credentialsProvider(username, password)

private fun credentialsProvider(username: String?, password: String?): CredentialsProvider =
    if (username != null && password != null) {
        UsernamePasswordCredentialsProvider(username, password)
    } else if (username == null) {
        // https://stackoverflow.com/questions/28073266/how-to-use-jgit-to-push-changes-to-remote-with-oauth-access-token
        UsernamePasswordCredentialsProvider(password, "")
    } else {
        CredentialsProvider.getDefault()
    }

private fun <R, T : GitCommand<*>> T.withRethrow(call: (T) -> R): R {
    try {
        return call(this)
    } catch (ex: GitAPIException) {
        throw IllegalStateException("Error in JGit API", ex)
    }
}
