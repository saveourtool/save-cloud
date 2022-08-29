/**
 * Git utilities that are used in preprocessor for download/clone/update git repo
 */

package com.saveourtool.save.preprocessor.utils

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.utils.debug
import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Instant

private val log = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

/**
 * @return default branch name
 * @throws IllegalStateException when failed to detect default branch name
 */
fun GitDto.detectDefaultBranchName() = Git.lsRemoteRepository()
    .setCredentialsProvider(credentialsProvider())
    .setRemote(url)
    .gitCallWithRethrow {
        it.callAsMap()[Constants.HEAD]
    }
    ?.takeIf { it.isSymbolic }
    ?.target
    ?.name
    ?.also { defaultBranch ->
        log.debug { "Getting default branch name $defaultBranch for httpUrl $url" }
    }
    ?.replace(Constants.R_HEADS, "")
    ?: throw IllegalStateException("Couldn't detect default branch name for $url")

/**
 * @param branch
 * @param pathToDirectory
 * @return commit timestamp as [Instant]
 * @throws IllegalStateException
 */
fun GitDto.cloneBranchToDirectory(branch: String, pathToDirectory: Path): Instant = doCloneToDirectory(branch, Constants.R_HEADS, pathToDirectory)

/**
 * @param tagName
 * @param pathToDirectory
 * @return commit timestamp as [Instant]
 * @throws IllegalStateException
 */
fun GitDto.cloneTagToDirectory(tagName: String, pathToDirectory: Path): Instant = doCloneToDirectory(tagName, Constants.R_TAGS, pathToDirectory)

private fun GitDto.doCloneToDirectory(
    branch: String,
    branchToClonePrefix: String,
    pathToDirectory: Path
): Instant = Git.cloneRepository()
    .setCredentialsProvider(credentialsProvider())
    .setURI(url)
    .setDirectory(pathToDirectory.toFile())
    .setRemote(Constants.DEFAULT_REMOTE_NAME)
    .setNoCheckout(false)
    .setBranch(branch)
    .setCloneAllBranches(false)
    .setBranchesToClone(listOf("$branchToClonePrefix$branch"))
    .callWithRethrow()
    .use { git ->
        withRethrow {
            val objectId = git.repository.resolve(Constants.HEAD)
            RevWalk(git.repository).use {
                it.parseCommit(objectId)
                    .authorIdent
                    .whenAsInstant
            }
        }
    }

/**
 * Sorted set of tags for [GitDto]
 *
 * @return list of tags
 * @throws IllegalStateException
 */
fun GitDto.detectTagList(): Collection<String> = Git.lsRemoteRepository()
    .setCredentialsProvider(credentialsProvider())
    .setRemote(url)
    .setHeads(false)
    .setTags(true)
    .gitCallWithRethrow { it.callAsMap() }
    .filterKeys { it.startsWith(Constants.R_TAGS) }
    .mapKeys { (key, _) -> key.removePrefix(Constants.R_TAGS) }
    .mapValues { (_, value) -> value.objectId.name }
    .toSortedMap()
    .entries
    .map { it.toPair() }
    .map { it.first }

private fun GitDto.credentialsProvider(): CredentialsProvider? = when {
    username != null && password != null -> UsernamePasswordCredentialsProvider(username, password)
    // https://stackoverflow.com/questions/28073266/how-to-use-jgit-to-push-changes-to-remote-with-oauth-access-token
    username == null && password != null -> UsernamePasswordCredentialsProvider(password, "")
    username == null && password == null -> CredentialsProvider.getDefault()
    else -> throw NotImplementedError("Unexpected git credentials")
}

private fun <R, T : GitCommand<*>> T.gitCallWithRethrow(call: (T) -> R): R = withRethrow {
    call(this)
}

private fun <R, T : GitCommand<R>> T.callWithRethrow(): R = withRethrow {
    this.call()
}

private fun <R> withRethrow(action: () -> R): R {
    try {
        return action()
    } catch (ex: GitAPIException) {
        throw IllegalStateException("Error in JGit API", ex)
    }
}
