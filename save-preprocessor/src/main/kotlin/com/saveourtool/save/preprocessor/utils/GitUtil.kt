/**
 * Git utilities that are used in preprocessor for download/clone/update git repo
 */

package com.saveourtool.save.preprocessor.utils

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

/**
 * @return latest commit
 */
fun com.saveourtool.save.testsuite.GitLocation.detectLatestSha1(): String = detectLatestSha1(credentialsProvider(), httpUrl, branch)

@SuppressWarnings("MaxLineLength")
/**
 * [How to use JGit to push changes to remote with OAuth access token](https://stackoverflow.com/questions/28073266/how-to-use-jgit-to-push-changes-to-remote-with-oauth-access-token)
 *
 * @return [CredentialsProvider]
 */
fun com.saveourtool.save.testsuite.GitLocation.credentialsProvider(): CredentialsProvider = credentialsProvider(username, token)

private fun <R, T : GitCommand<*>> T.withRethrow(call: (T) -> R): R {
    try {
        return call(this)
    } catch (ex: GitAPIException) {
        throw IllegalStateException("Error in JGit API", ex)
    }
}

/**
 * @param httpUrl
 * @param token
 * @param username
 * @return default branch name
 * @throws IllegalStateException
 */
fun detectDefaultBranchName(httpUrl: String, username: String?, token: String?): String {
    val head: Ref? = Git.lsRemoteRepository()
        .setCredentialsProvider(credentialsProvider(username, token))
        // ls without clone
        // FIXME: need to extract to a common place
        .setRemote(httpUrl)
        .withRethrow {
            it.callAsMap()[Constants.HEAD]
        }

    val defaultBranch = head?.let {
        if (head.isSymbolic) {
            // Branch name
            head.target.name
        } else {
            // FIXME: probably we should throw Exception here
            // SHA-1 hash
            head.objectId.name
        }
    } ?: throw IllegalStateException("Couldn't detect default branch name for $httpUrl")

    log.debug("Getting default branch name: $defaultBranch")

    return defaultBranch.replace(Constants.R_HEADS, "${Constants.DEFAULT_REMOTE_NAME}/")
}

private fun detectLatestSha1(credentialsProvider: CredentialsProvider, httpUrl: String, branchName: String): String = Git.lsRemoteRepository()
    .setCredentialsProvider(credentialsProvider)
    .setRemote(httpUrl)
    .withRethrow {
        it.callAsMap()["${Constants.R_HEADS}$branchName"]
    }
    ?.objectId
    ?.name
    ?: throw IllegalStateException("Couldn't detect hash of ${Constants.HEAD} for $httpUrl/$branchName")

private fun credentialsProvider(username: String?, token: String?): CredentialsProvider =
        if (username != null && token != null) {
            UsernamePasswordCredentialsProvider(username, token)
        } else {
            CredentialsProvider.getDefault()
        }
