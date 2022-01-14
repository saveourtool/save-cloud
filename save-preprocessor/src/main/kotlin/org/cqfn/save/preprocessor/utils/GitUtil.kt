/**
 * Git utilities that are used in preprocessor for download/clone/update git repo
 */

package org.cqfn.save.preprocessor.utils

import org.cqfn.save.entities.GitDto
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

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
    return Git.cloneRepository()
        .setURI(gitDto.url)
        .setCredentialsProvider(userCredentials)
        .setDirectory(tmpDir)
        .call()
}
