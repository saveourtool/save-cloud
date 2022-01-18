/**
 * Git utilities that are used in preprocessor for download/clone/update git repo
 */

package org.cqfn.save.preprocessor.utils

import org.cqfn.save.entities.GitDto
import org.eclipse.jgit.api.Git
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
        // FixMe will be pulled the current branch?
        git.pull().setCredentialsProvider(userCredentials).call()
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
