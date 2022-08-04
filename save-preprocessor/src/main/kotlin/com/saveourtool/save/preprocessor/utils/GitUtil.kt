/**
 * Git utilities that are used in preprocessor for download/clone/update git repo
 */

package com.saveourtool.save.preprocessor.utils

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.utils.debug
import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
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
    // ls without clone
    // FIXME: need to extract to a common place
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
 * @return latest commit
 */
fun GitDto.detectLatestSha1(branch: String): String = Git.lsRemoteRepository()
    .setCredentialsProvider(credentialsProvider())
    .setRemote(url)
    .gitCallWithRethrow {
        it.callAsMap()["${Constants.R_HEADS}$branch"]
    }
    ?.objectId
    ?.name
    ?: throw IllegalStateException("Couldn't detect hash of ${Constants.HEAD} for $url/$branch")

/**
 * @param branch
 * @param sha1
 * @param pathToDirectory
 * @return commit timestamp as [Instant]
 * @throws IllegalStateException
 */
fun GitDto.cloneToDirectory(branch: String, sha1: String, pathToDirectory: Path): Instant = Git.cloneRepository()
    .setCredentialsProvider(credentialsProvider())
    .setURI(url)
    .setDirectory(pathToDirectory.toFile())
    .setRemote(Constants.DEFAULT_REMOTE_NAME)
    .setNoCheckout(true)
    .setNoTags()
    .setBranch(branch)
    .setCloneAllBranches(false)
    .callWithRethrow()
    .use { git ->
        git.checkout()
            .setName(sha1)
            .callWithRethrow()
        withRethrow {
            RevWalk(git.repository).use {
                it.parseCommit(ObjectId.fromString(sha1))
                    .authorIdent
                    .whenAsInstant
            }
        }
    }

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
